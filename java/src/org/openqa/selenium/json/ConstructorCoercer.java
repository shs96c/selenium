// Licensed to the Software Freedom Conservancy (SFC) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The SFC licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.openqa.selenium.json;

import static org.openqa.selenium.json.Types.narrow;

import java.beans.ConstructorProperties;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import org.jspecify.annotations.Nullable;
import org.openqa.selenium.internal.Require;

/**
 * A {@link TypeCoercer} that deserializes JSON objects into immutable Java classes by matching JSON
 * keys to constructor parameter names.
 *
 * <p>Requires compilation with {@code -parameters} so that constructor parameter names are
 * available via reflection. Classes may also declare {@link ConstructorProperties} to specify JSON
 * field names when they differ from constructor parameter names.
 *
 * <p>For classes where JSON keys differ from parameter names, prefer {@link ConstructorProperties}.
 */
class ConstructorCoercer extends TypeCoercer<Object> {

  private final JsonTypeCoercer coercer;

  ConstructorCoercer(JsonTypeCoercer coercer) {
    this.coercer = Require.nonNull("Coercer", coercer);
  }

  @Override
  public boolean test(Class<?> aClass) {
    if (aClass.isInterface()
        || aClass.isEnum()
        || aClass.isPrimitive()
        || aClass.isArray()
        || Modifier.isAbstract(aClass.getModifiers())) {
      return false;
    }
    return findConstructor(aClass) != null;
  }

  @Override
  public BiFunction<JsonInput, PropertySetting, Object> apply(Type type) {
    Class<?> aClass = narrow(type);
    Constructor<?> constructor = findConstructor(aClass);
    if (constructor == null) {
      throw new JsonException("No suitable constructor found for " + type);
    }
    constructor.setAccessible(true);

    Parameter[] params = constructor.getParameters();
    String[] jsonNames = getJsonNames(constructor);

    Map<String, ParamTarget> jsonKeyToParam = new HashMap<>();
    for (int i = 0; i < params.length; i++) {
      ParamTarget target = new ParamTarget(i, params[i].getParameterizedType());
      jsonKeyToParam.put(jsonNames[i], target);
      if (!params[i].getName().equals(jsonNames[i])) {
        jsonKeyToParam.put(params[i].getName(), target);
      }
    }

    return (jsonInput, setter) -> {
      Object[] args = new Object[params.length];
      // Initialize primitives to their defaults
      for (int i = 0; i < params.length; i++) {
        if (params[i].getType().isPrimitive()) {
          args[i] = defaultForPrimitive(params[i].getType());
        } else if (Optional.class.equals(params[i].getType())) {
          args[i] = Optional.empty();
        }
      }
      boolean[] seen = new boolean[params.length];

      jsonInput.beginObject();
      while (jsonInput.hasNext()) {
        String key = jsonInput.nextName();
        ParamTarget target = jsonKeyToParam.get(key);
        if (target != null) {
          args[target.index] = coercer.coerce(jsonInput, target.type, setter);
          seen[target.index] = true;
        } else {
          jsonInput.skipValue();
        }
      }
      jsonInput.endObject();

      for (int i = 0; i < params.length; i++) {
        if (!seen[i] && isMandatory(params[i])) {
          throw new JsonException("Missing required JSON field '" + jsonNames[i] + "' for " + type);
        }
      }

      try {
        return constructor.newInstance(args);
      } catch (ReflectiveOperationException e) {
        throw new JsonException("Unable to create instance of " + type, e);
      }
    };
  }

  /** Find the best constructor for immutable JSON binding. */
  private static Constructor<?> findConstructor(Class<?> aClass) {
    return findConstructorWithProperties(aClass) != null
        ? findConstructorWithProperties(aClass)
        : Arrays.stream(aClass.getDeclaredConstructors())
            .filter(c -> c.getParameterCount() > 0)
            .filter(c -> !c.isSynthetic())
            .filter(ConstructorCoercer::allParamsNamed)
            .max(Comparator.comparingInt(Constructor::getParameterCount))
            .orElse(null);
  }

  static @Nullable Constructor<?> findConstructorWithProperties(Class<?> aClass) {
    return Arrays.stream(aClass.getDeclaredConstructors())
        .filter(c -> c.getParameterCount() > 0)
        .filter(c -> !c.isSynthetic())
        .filter(ConstructorCoercer::hasValidConstructorProperties)
        .max(Comparator.comparingInt(Constructor::getParameterCount))
        .orElse(null);
  }

  private static boolean allParamsNamed(Constructor<?> constructor) {
    for (Parameter param : constructor.getParameters()) {
      if (!param.isNamePresent()) {
        return false;
      }
    }
    return true;
  }

  private static boolean hasValidConstructorProperties(Constructor<?> constructor) {
    ConstructorProperties properties = constructor.getAnnotation(ConstructorProperties.class);
    return properties != null && properties.value().length == constructor.getParameterCount();
  }

  static String[] getJsonNames(Constructor<?> constructor) {
    ConstructorProperties properties = constructor.getAnnotation(ConstructorProperties.class);
    if (properties != null) {
      if (properties.value().length != constructor.getParameterCount()) {
        throw new JsonException(
            "ConstructorProperties length does not match parameter count for " + constructor);
      }
      return properties.value();
    }

    return Arrays.stream(constructor.getParameters())
        .map(Parameter::getName)
        .toArray(String[]::new);
  }

  private static boolean isMandatory(Parameter param) {
    if (param.getType().isPrimitive() || Optional.class.equals(param.getType())) {
      return false;
    }
    return param.getAnnotation(Nullable.class) == null
        && param.getAnnotatedType().getAnnotation(Nullable.class) == null;
  }

  private static Object defaultForPrimitive(Class<?> type) {
    if (type == boolean.class) return false;
    if (type == byte.class) return (byte) 0;
    if (type == char.class) return (char) 0;
    if (type == short.class) return (short) 0;
    if (type == int.class) return 0;
    if (type == long.class) return 0L;
    if (type == float.class) return 0.0f;
    if (type == double.class) return 0.0d;
    return null;
  }

  private static class ParamTarget {
    final int index;
    final Type type;

    ParamTarget(int index, Type type) {
      this.index = index;
      this.type = type;
    }
  }
}

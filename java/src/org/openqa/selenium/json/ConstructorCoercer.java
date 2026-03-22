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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import org.openqa.selenium.internal.Require;

/**
 * A {@link TypeCoercer} that deserializes JSON objects into immutable Java classes by matching JSON
 * keys to constructor parameter names.
 *
 * <p>Requires compilation with {@code -parameters} so that constructor parameter names are
 * available via reflection.
 *
 * <p>For classes where JSON keys differ from parameter names, a static {@code jsonAliases()} method
 * returning {@code Map<String, String>} (JSON key → parameter name) provides the mapping.
 */
class ConstructorCoercer extends TypeCoercer<Object> {

  private static final String ALIASES_METHOD_NAME = "jsonAliases";

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
    Map<String, String> aliases = getAliases(aClass);

    // Build mapping: JSON key → parameter index
    // Also build reverse: parameter name → index for alias lookups
    Map<String, Integer> paramNameToIndex = new HashMap<>();
    for (int i = 0; i < params.length; i++) {
      paramNameToIndex.put(params[i].getName(), i);
    }

    // JSON key → (parameter index, parameter generic type)
    Map<String, ParamTarget> jsonKeyToParam = new HashMap<>();
    // Direct matches: JSON key == parameter name
    for (int i = 0; i < params.length; i++) {
      jsonKeyToParam.put(params[i].getName(), new ParamTarget(i, params[i].getParameterizedType()));
    }
    // Alias overrides: JSON key → parameter name (via jsonAliases())
    for (Map.Entry<String, String> alias : aliases.entrySet()) {
      String jsonKey = alias.getKey();
      String paramName = alias.getValue();
      Integer idx = paramNameToIndex.get(paramName);
      if (idx != null) {
        jsonKeyToParam.put(jsonKey, new ParamTarget(idx, params[idx].getParameterizedType()));
      }
    }

    return (jsonInput, setter) -> {
      Object[] args = new Object[params.length];
      // Initialize primitives to their defaults
      for (int i = 0; i < params.length; i++) {
        if (params[i].getType().isPrimitive()) {
          args[i] = defaultForPrimitive(params[i].getType());
        }
      }

      jsonInput.beginObject();
      while (jsonInput.hasNext()) {
        String key = jsonInput.nextName();
        ParamTarget target = jsonKeyToParam.get(key);
        if (target != null) {
          args[target.index] = coercer.coerce(jsonInput, target.type, setter);
        } else {
          jsonInput.skipValue();
        }
      }
      jsonInput.endObject();

      try {
        return constructor.newInstance(args);
      } catch (ReflectiveOperationException e) {
        throw new JsonException("Unable to create instance of " + type, e);
      }
    };
  }

  /**
   * Find the best constructor: the one with the most parameters where all parameter names are
   * present (compiled with -parameters).
   */
  private static Constructor<?> findConstructor(Class<?> aClass) {
    return Arrays.stream(aClass.getDeclaredConstructors())
        .filter(c -> c.getParameterCount() > 0)
        .filter(c -> !c.isSynthetic())
        .filter(ConstructorCoercer::allParamsNamed)
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

  @SuppressWarnings("unchecked")
  private static Map<String, String> getAliases(Class<?> aClass) {
    try {
      Method method = aClass.getDeclaredMethod(ALIASES_METHOD_NAME);
      if (Modifier.isStatic(method.getModifiers())
          && Map.class.isAssignableFrom(method.getReturnType())) {
        method.setAccessible(true);
        return (Map<String, String>) method.invoke(null);
      }
    } catch (NoSuchMethodException e) {
      // No aliases declared — that's fine
    } catch (ReflectiveOperationException e) {
      throw new JsonException("Unable to read jsonAliases() from " + aClass.getName(), e);
    }
    return Collections.emptyMap();
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

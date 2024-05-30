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

'use strict'

const fs = require('node:fs')
const path = require('node:path')
const { runfiles } = require('@bazel/runfiles')

/**
 * Locates a test resource.
 * @param {string} filePath The file to locate from the root of the project.
 * @return {string} The full path for the file, if it exists.
 * @throws {Error} If the file does not exist.
 */
exports.locate = function (filePath) {
  // Try the easy path first: the filePath is relative to the cwd and exists
  const resolvedPath = path.resolve(filePath)

  if (fs.existsSync(resolvedPath)) {
    return resolvedPath
  }

  // Can we find this with runfiles normally?
  try {
    return runfiles.resolve(filePath)
  } catch {
    // This is fine. The `runfiles` library does this when it can't find things
  }

  // Is the item in the workspace?
  try {
    return runfiles.resolveWorkspaceRelative(filePath)
  } catch {
    // Fall through
  }

  // Find the repo mapping file
  let repoMappingFile
  try {
    repoMappingFile = runfiles.resolve('_repo_mapping')
  } catch {
    throw new Error('Unable to locate ' + filePath)
  }
  const lines = fs.readFileSync(repoMappingFile, {encoding: 'utf8'}).split('\n')

  // Build a map of "repo we declared we need" to "path"
  const mapping = {}
  for (const line of lines) {
    if (line.startsWith(',')) {
      const parts = line.split(',', 3)
      mapping[parts[1]] = parts[2]
    }
  }

  // Get the first segment of the path
  const pathSegments = filePath.split('/')
  if (!pathSegments.length) {
    throw new Error('Unable to locate ' + filePath)
  }

  pathSegments[0] = mapping[pathSegments[0]] ? mapping[pathSegments[0]] : '_main'

  return runfiles.resolve(path.join(...pathSegments))
}

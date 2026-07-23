package com.cag.core

import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile

internal fun isAncestor(ancestor: VirtualFile, child: VirtualFile): Boolean =
    VfsUtil.isAncestor(ancestor, child, true)

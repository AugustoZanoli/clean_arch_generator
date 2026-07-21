package com.cag.core

import com.intellij.openapi.vfs.VirtualFile

internal fun isAncestor(ancestor: VirtualFile, child: VirtualFile): Boolean {
    var cur: VirtualFile? = child
    while (cur != null) {
        if (cur == ancestor) return true
        cur = cur.parent
    }
    return false
}

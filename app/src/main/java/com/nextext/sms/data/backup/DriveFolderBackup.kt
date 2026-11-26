package com.nextext.sms.data.backup

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import java.io.FileNotFoundException

/**
 * Writes a single JSON document into a user-picked Drive folder through the
 * Storage Access Framework. The Google Drive app owns the network sync, so this
 * path needs no OAuth client and no network code of its own.
 */
object DriveFolderBackup {
    const val FILE_NAME = "nextext-backup.json"
    private const val MIME_JSON = "application/json"

    fun write(context: Context, tree: Uri, bytes: ByteArray): Result<Unit> = runCatching {
        if (!hasPersistedWrite(context, tree)) {
            throw SecurityException("Drive folder access was revoked — link the folder again")
        }
        val resolver = context.contentResolver
        val rootId = DocumentsContract.getTreeDocumentId(tree)
        val rootDoc = DocumentsContract.buildDocumentUriUsingTree(tree, rootId)
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, rootId)

        val target = findDocument(context, children, tree)
        val doc = target ?: DocumentsContract.createDocument(
            resolver, rootDoc, MIME_JSON, FILE_NAME
        ) ?: throw FileNotFoundException("Drive folder is not writable")

        resolver.openOutputStream(doc, "wt")?.use { stream ->
            stream.write(bytes)
            stream.flush()
        } ?: throw FileNotFoundException("Drive folder is not writable")
    }

    fun read(context: Context, tree: Uri): Result<ByteArray> = runCatching {
        if (!hasPersistedWrite(context, tree)) {
            throw SecurityException("Drive folder access was revoked — link the folder again")
        }
        val resolver = context.contentResolver
        val rootId = DocumentsContract.getTreeDocumentId(tree)
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, rootId)

        val doc = findDocument(context, children, tree)
            ?: throw FileNotFoundException("No $FILE_NAME in the linked Drive folder")

        resolver.openInputStream(doc)?.use { it.readBytes() }
            ?: throw FileNotFoundException("Cannot read $FILE_NAME from Drive")
    }

    fun queryFolderName(context: Context, tree: Uri): String? = runCatching {
        val doc = DocumentsContract.buildDocumentUriUsingTree(
            tree, DocumentsContract.getTreeDocumentId(tree)
        )
        context.contentResolver.query(
            doc,
            arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull()

    fun hasPersistedWrite(context: Context, tree: Uri): Boolean {
        val target = tree.toString().trimEnd('/')
        return context.contentResolver.persistedUriPermissions.any { perm ->
            perm.isWritePermission && perm.uri.toString().trimEnd('/') == target
        }
    }

    private fun findDocument(context: Context, children: Uri, tree: Uri): Uri? {
        context.contentResolver.query(
            children,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME
            ),
            null, null, null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                if (cursor.getString(1) == FILE_NAME) {
                    return DocumentsContract.buildDocumentUriUsingTree(tree, cursor.getString(0))
                }
            }
        }
        return null
    }
}

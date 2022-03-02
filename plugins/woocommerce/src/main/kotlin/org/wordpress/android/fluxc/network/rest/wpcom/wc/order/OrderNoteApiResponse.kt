package org.wordpress.android.fluxc.network.rest.wpcom.wc.order

import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.OrderNoteEntity
import org.wordpress.android.fluxc.network.Response

@Suppress("PropertyName")
class OrderNoteApiResponse : Response {
    val id: Long? = null
    val date_created_gmt: String? = null
    val note: String? = null
    val author: String? = null
    // If true, the note will be shown to customers and they will be notified. If false, the note will be for admin
    // reference only. Default is false.
    val customer_note: Boolean = false
}

fun OrderNoteApiResponse.toDataModel(localSiteId: LocalId, orderId: RemoteId): OrderNoteEntity =
        OrderNoteEntity(
                localSiteId = localSiteId,
                noteId = RemoteId(id ?: 0),
                orderId = orderId,
                dateCreated = date_created_gmt?.let { "${it}Z" } ?: "",
                note = note ?: "",
                isSystemNote = author == "system" || author == "WooCommerce",
                author = author ?: "",
                isCustomerNote = customer_note
        )

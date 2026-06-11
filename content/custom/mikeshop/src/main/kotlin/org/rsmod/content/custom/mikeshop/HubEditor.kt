package org.rsmod.content.custom.mikeshop

import org.rsmod.api.type.editors.npc.NpcEditor

/** Nette namen voor de hub-NPC's, zodat je in-game meteen ziet wie wat doet. */
internal object HubEditor : NpcEditor() {
    init {
        edit(HubNpcs.clerk) { name = "Shop Clerk" }
        edit(HubNpcs.wizard) { name = "Teleport Wizard" }
    }
}

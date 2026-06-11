package org.rsmod.content.custom.mikeshop

import org.rsmod.api.config.refs.content
import org.rsmod.api.type.editors.npc.NpcEditor

/**
 * Maakt de PK-NPC's "echt":
 *  - banker1 krijgt de content.banker-groep, zodat de vanilla Banker-handler (Talk/Bank/Collect)
 *    werkt -> de bank werkt nu op BEIDE worlds (gedeelde content + cache).
 *  - de 4 shop-clerks krijgen duidelijke namen, zodat je in Edgeville meteen ziet welke winkel het is.
 *
 * (Type-wijziging -> vereist packCache.)
 */
internal object PkEditor : NpcEditor() {
    init {
        edit(PkClerkNpcs.banker) { contentGroup = content.banker }
        edit(PkClerkNpcs.melee) { name = "PK Melee Shop" }
        edit(PkClerkNpcs.ranged) { name = "PK Ranged Shop" }
        edit(PkClerkNpcs.magic) { name = "PK Magic Shop" }
        edit(PkClerkNpcs.supply) { name = "PK Supplies Shop" }
    }
}

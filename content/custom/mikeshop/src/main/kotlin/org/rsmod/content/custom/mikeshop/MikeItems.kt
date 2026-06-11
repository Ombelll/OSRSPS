package org.rsmod.content.custom.mikeshop

import org.rsmod.api.config.refs.objs
import org.rsmod.api.type.editors.obj.ObjEditor

/**
 * Een "custom item" door een bestaand item te RESKINNEN.
 *
 * Waarom reskin en niet 100% nieuw? Een volledig nieuw item heeft een eigen 3D-model
 * nodig in de cache (cache-editing = een groter onderwerp). De standaard manier om
 * zonder dat een custom item te maken is: neem een bestaand item en overschrijf alleen
 * naam, beschrijving en waarde. Het model blijft werken, want de ObjEditor overschrijft
 * ALLEEN de velden die je hier zet — al het andere blijft van het basis-item.
 */
internal object MikeItems : ObjEditor() {
    init {
        edit(objs.cabbage) {
            name = "Mike's Lucky Cabbage"
            desc = "A legendary cabbage, blessed on your very own private server."
            cost = 5000
        }
    }
}

package org.rsmod.content.custom.mikeshop

import jakarta.inject.Inject
import org.rsmod.api.db.Database
import org.rsmod.api.db.DatabaseConnection
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.GameMessage
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.script.onCommand
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private data class SocialSnapshot(
    val friends: Map<String, Set<String>>,
    val clans: List<ClanChannel>,
)

private data class ClanChannel(
    val name: String,
    val ownerKey: String,
    val members: MutableSet<String>,
)

class SocialCommands
@Inject
constructor(
    private val players: PlayerList,
    private val protectedAccess: ProtectedAccessLauncher,
    private val database: Database,
) : PluginScript() {
    private val friends = mutableMapOf<String, MutableSet<String>>()
    private val lastPrivateMessageFrom = mutableMapOf<String, String>()
    private val clanByOwner = mutableMapOf<String, ClanChannel>()
    private val clanByMember = mutableMapOf<String, ClanChannel>()
    private var loaded = false

    override fun ScriptContext.startup() {
        onCommand("online") {
            desc = "List online players"
            cheat { player.showOnlinePlayers() }
        }
        onCommand("pm") {
            desc = "Send a private command-message: ::pm player message"
            cheat { player.sendPrivateMessage(args.joinToString(" ").trim()) }
        }
        onCommand("reply") {
            desc = "Reply to the last private command-message"
            cheat { player.replyPrivateMessage(args.joinToString(" ").trim()) }
        }
        onCommand("friendadd") {
            desc = "Add a persistent friend: ::friendadd player"
            cheat {
                protectedAccess.launch(player) {
                    ensureLoaded()
                    if (player.addFriend(args.joinToString(" ").trim())) {
                        saveSocial()
                    }
                }
            }
        }
        onCommand("frienddel") {
            desc = "Remove a persistent friend: ::frienddel player"
            cheat {
                protectedAccess.launch(player) {
                    ensureLoaded()
                    if (player.removeFriend(args.joinToString(" ").trim())) {
                        saveSocial()
                    }
                }
            }
        }
        onCommand("friends") {
            desc = "List persistent friends"
            cheat {
                protectedAccess.launch(player) {
                    ensureLoaded()
                    player.listFriends()
                }
            }
        }
        onCommand("clancreate") {
            desc = "Create a persistent command clan-chat"
            cheat {
                protectedAccess.launch(player) {
                    ensureLoaded()
                    if (player.createClan(args.joinToString(" ").trim())) {
                        saveSocial()
                    }
                }
            }
        }
        onCommand("clanjoin") {
            desc = "Join a command clan-chat: ::clanjoin owner"
            cheat {
                protectedAccess.launch(player) {
                    ensureLoaded()
                    if (player.joinClan(args.joinToString(" ").trim())) {
                        saveSocial()
                    }
                }
            }
        }
        onCommand("clanleave") {
            desc = "Leave your command clan-chat"
            cheat {
                protectedAccess.launch(player) {
                    ensureLoaded()
                    if (player.leaveClan()) {
                        saveSocial()
                    }
                }
            }
        }
        onCommand("clanmsg") {
            desc = "Send a command clan-chat message"
            cheat {
                protectedAccess.launch(player) {
                    ensureLoaded()
                    player.clanMessage(args.joinToString(" ").trim())
                }
            }
        }
        onCommand("clanwho") {
            desc = "List command clan-chat members"
            cheat {
                protectedAccess.launch(player) {
                    ensureLoaded()
                    player.clanWho()
                }
            }
        }
    }

    private fun Player.showOnlinePlayers() {
        val names = players.map { it.displayName }.sortedWith(String.CASE_INSENSITIVE_ORDER)
        mes("Online (${names.size}): ${names.joinToString(", ")}")
    }

    private fun Player.sendPrivateMessage(input: String) {
        val parsed = parsePlayerAndMessage(input)
        if (parsed == null) {
            mes("Usage: ::pm player message")
            return
        }
        val (target, message) = parsed
        if (target === this) {
            mes("You cannot send a private message to yourself.")
            return
        }
        deliverPrivateMessage(target, message)
    }

    private fun Player.replyPrivateMessage(message: String) {
        if (message.isBlank()) {
            mes("Usage: ::reply message")
            return
        }
        val targetKey = lastPrivateMessageFrom[chatKey()]
        val target = targetKey?.let(::onlineByKey)
        if (target == null) {
            mes("There is nobody online to reply to.")
            return
        }
        deliverPrivateMessage(target, message)
    }

    private fun Player.deliverPrivateMessage(target: Player, message: String) {
        val cleanMessage = message.take(MAX_MESSAGE_LENGTH)
        GameMessage.requestMes(target, cleanMessage, displayName, ChatType.PrivateChat)
        mes("To ${target.displayName}: $cleanMessage", ChatType.PrivateChatOut)
        lastPrivateMessageFrom[target.chatKey()] = chatKey()
    }

    private fun Player.addFriend(name: String): Boolean {
        val target = findOnlinePlayer(name)
        if (target == null) {
            mes("Could not find an online player named '$name'.")
            return false
        }
        if (target === this) {
            mes("You are already your own best contact here.")
            return false
        }
        val added = friendSet().add(target.chatKey())
        if (!added) {
            mes("${target.displayName} is already in your friends list.")
            return false
        }
        mes("${target.displayName} added to your persistent friends.", ChatType.FriendNotification)
        target.mes("${displayName} added you as a friend.", ChatType.FriendNotification)
        return true
    }

    private fun Player.removeFriend(name: String): Boolean {
        val key = normalize(name)
        if (key.isBlank()) {
            mes("Usage: ::frienddel player")
            return false
        }
        val removed = friendSet().remove(key)
        if (removed) {
            mes("Removed $name from your persistent friends.", ChatType.FriendNotification)
        } else {
            mes("$name is not in your friends list.")
        }
        return removed
    }

    private fun Player.listFriends() {
        val set = friendSet()
        if (set.isEmpty()) {
            mes("Friends: none. Add one with ::friendadd player.")
            return
        }
        val names =
            set.sorted().map { key ->
                val online = onlineByKey(key)
                online?.displayName ?: "$key (offline)"
            }
        mes("Friends: ${names.joinToString(", ")}")
    }

    private fun Player.createClan(rawName: String): Boolean {
        val ownerKey = chatKey()
        if (clanByOwner.containsKey(ownerKey)) {
            mes("You already own a clan-chat. Use ::clanwho or ::clanleave.")
            return false
        }
        leaveClan(silent = true)
        val name = rawName.ifBlank { "$displayName chat" }.take(MAX_CLAN_NAME_LENGTH)
        val clan = ClanChannel(name = name, ownerKey = ownerKey, members = mutableSetOf(ownerKey))
        clanByOwner[ownerKey] = clan
        clanByMember[ownerKey] = clan
        mes("Clan-chat created: $name. Others can use ::clanjoin $displayName.", ChatType.ClanChat)
        return true
    }

    private fun Player.joinClan(name: String): Boolean {
        val owner = findOnlinePlayer(name)
        val clan = owner?.let { clanByOwner[it.chatKey()] }
        if (clan == null) {
            mes("Could not find an online clan owner named '$name'.")
            return false
        }
        leaveClan(silent = true)
        val added = clan.members.add(chatKey())
        clanByMember[chatKey()] = clan
        if (added) {
            clan.broadcast("${displayName} joined ${clan.name}.")
        }
        return added
    }

    private fun Player.leaveClan(silent: Boolean = false): Boolean {
        val key = chatKey()
        val clan = clanByMember.remove(key)
        if (clan == null) {
            if (!silent) {
                mes("You are not in a clan-chat.")
            }
            return false
        }
        clan.members.remove(key)
        if (clan.ownerKey == key) {
            clan.members.toList().forEach { clanByMember.remove(it) }
            clanByOwner.remove(key)
            if (!silent) {
                mes("Your clan-chat has been closed.", ChatType.ClanChat)
            }
            return true
        }
        if (!silent) {
            mes("You leave ${clan.name}.", ChatType.ClanChat)
        }
        clan.broadcast("${displayName} left ${clan.name}.")
        return true
    }

    private fun Player.clanMessage(message: String) {
        if (message.isBlank()) {
            mes("Usage: ::clanmsg message")
            return
        }
        val clan = clanByMember[chatKey()]
        if (clan == null) {
            mes("You are not in a clan-chat. Use ::clancreate or ::clanjoin owner.")
            return
        }
        clan.broadcast("[${nameFor(clan.ownerKey)}] ${displayName}: ${message.take(MAX_MESSAGE_LENGTH)}")
    }

    private fun Player.clanWho() {
        val clan = clanByMember[chatKey()]
        if (clan == null) {
            mes("You are not in a clan-chat.")
            return
        }
        val names = clan.members.map { nameFor(it) }.sortedWith(String.CASE_INSENSITIVE_ORDER)
        mes("${clan.name} (${names.size}): ${names.joinToString(", ")}", ChatType.ClanChat)
    }

    private fun Player.parsePlayerAndMessage(input: String): Pair<Player, String>? {
        if (input.isBlank()) {
            return null
        }
        val match =
            players
                .filter { it !== this }
                .sortedByDescending { it.displayName.length }
                .firstOrNull { input.startsWith(it.displayName, ignoreCase = true) }
                ?: return null
        val message = input.drop(match.displayName.length).trim()
        if (message.isBlank()) {
            return null
        }
        return match to message
    }

    private fun Player.friendSet(): MutableSet<String> = friends.getOrPut(chatKey()) { mutableSetOf() }

    private suspend fun ensureLoaded() {
        if (loaded) {
            return
        }
        val snapshot = database.withTransaction { connection -> connection.loadSocial() }
        friends.clear()
        clanByOwner.clear()
        clanByMember.clear()
        snapshot.friends.forEach { (owner, values) -> friends[owner] = values.toMutableSet() }
        snapshot.clans.forEach { clan ->
            val copy = clan.copy(members = clan.members.toMutableSet())
            clanByOwner[copy.ownerKey] = copy
            copy.members.forEach { clanByMember[it] = copy }
        }
        loaded = true
    }

    private suspend fun saveSocial() {
        val snapshot =
            SocialSnapshot(
                friends = friends.mapValues { (_, values) -> values.toSet() },
                clans = clanByOwner.values.map { it.copy(members = it.members.toMutableSet()) },
            )
        database.withTransaction { connection -> connection.saveSocial(snapshot) }
    }

    private fun DatabaseConnection.loadSocial(): SocialSnapshot {
        val loadedFriends = linkedMapOf<String, MutableSet<String>>()
        prepareStatement("SELECT owner, friend FROM social_friends ORDER BY owner, friend").use { statement ->
            statement.executeQuery().use { rows ->
                while (rows.next()) {
                    loadedFriends
                        .getOrPut(rows.getString("owner")) { linkedSetOf() }
                        .add(rows.getString("friend"))
                }
            }
        }

        val clans = linkedMapOf<String, ClanChannel>()
        prepareStatement("SELECT owner, name FROM social_clans ORDER BY owner").use { statement ->
            statement.executeQuery().use { rows ->
                while (rows.next()) {
                    val owner = rows.getString("owner")
                    clans[owner] = ClanChannel(rows.getString("name"), owner, linkedSetOf(owner))
                }
            }
        }
        prepareStatement("SELECT owner, member FROM social_clan_members ORDER BY owner, member").use { statement ->
            statement.executeQuery().use { rows ->
                while (rows.next()) {
                    val owner = rows.getString("owner")
                    val member = rows.getString("member")
                    clans[owner]?.members?.add(member)
                }
            }
        }
        return SocialSnapshot(loadedFriends, clans.values.toList())
    }

    private fun DatabaseConnection.saveSocial(snapshot: SocialSnapshot) {
        prepareStatement("DELETE FROM social_clan_members").use { it.executeUpdate() }
        prepareStatement("DELETE FROM social_clans").use { it.executeUpdate() }
        prepareStatement("DELETE FROM social_friends").use { it.executeUpdate() }

        prepareStatement("INSERT INTO social_friends (owner, friend) VALUES (?, ?)").use { statement ->
            snapshot.friends.forEach { (owner, values) ->
                values.forEach { friend ->
                    statement.setString(1, owner)
                    statement.setString(2, friend)
                    statement.addBatch()
                }
            }
            statement.executeBatch()
        }

        prepareStatement("INSERT INTO social_clans (owner, name) VALUES (?, ?)").use { statement ->
            snapshot.clans.forEach { clan ->
                statement.setString(1, clan.ownerKey)
                statement.setString(2, clan.name)
                statement.addBatch()
            }
            statement.executeBatch()
        }

        prepareStatement("INSERT INTO social_clan_members (owner, member) VALUES (?, ?)").use { statement ->
            snapshot.clans.forEach { clan ->
                clan.members.forEach { member ->
                    statement.setString(1, clan.ownerKey)
                    statement.setString(2, member)
                    statement.addBatch()
                }
            }
            statement.executeBatch()
        }
    }

    private fun findOnlinePlayer(name: String): Player? {
        val key = normalize(name)
        return players.firstOrNull { it.chatKey() == key }
    }

    private fun onlineByKey(key: String): Player? = players.firstOrNull { it.chatKey() == key }

    private fun nameFor(key: String): String = onlineByKey(key)?.displayName ?: key

    private fun Player.chatKey(): String = normalize(displayName)

    private fun normalize(name: String): String = name.trim().lowercase()

    private fun ClanChannel.broadcast(text: String) {
        members.mapNotNull(::onlineByKey).forEach { it.mes(text, ChatType.ClanChat) }
    }

    private companion object {
        const val MAX_MESSAGE_LENGTH = 120
        const val MAX_CLAN_NAME_LENGTH = 20
    }
}

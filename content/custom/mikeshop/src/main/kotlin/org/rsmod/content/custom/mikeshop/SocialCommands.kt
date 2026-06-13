package org.rsmod.content.custom.mikeshop

import jakarta.inject.Inject
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.GameMessage
import org.rsmod.api.player.output.mes
import org.rsmod.api.script.onCommand
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class SocialCommands @Inject constructor(private val players: PlayerList) : PluginScript() {
    private val friends = mutableMapOf<String, MutableSet<String>>()
    private val lastPrivateMessageFrom = mutableMapOf<String, String>()
    private val clanByOwner = mutableMapOf<String, ClanChannel>()
    private val clanByMember = mutableMapOf<String, ClanChannel>()

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
            desc = "Add a session friend: ::friendadd player"
            cheat { player.addFriend(args.joinToString(" ").trim()) }
        }
        onCommand("frienddel") {
            desc = "Remove a session friend: ::frienddel player"
            cheat { player.removeFriend(args.joinToString(" ").trim()) }
        }
        onCommand("friends") {
            desc = "List session friends"
            cheat { player.listFriends() }
        }
        onCommand("clancreate") {
            desc = "Create a command clan-chat"
            cheat { player.createClan(args.joinToString(" ").trim()) }
        }
        onCommand("clanjoin") {
            desc = "Join a command clan-chat: ::clanjoin owner"
            cheat { player.joinClan(args.joinToString(" ").trim()) }
        }
        onCommand("clanleave") {
            desc = "Leave your command clan-chat"
            cheat { player.leaveClan() }
        }
        onCommand("clanmsg") {
            desc = "Send a command clan-chat message"
            cheat { player.clanMessage(args.joinToString(" ").trim()) }
        }
        onCommand("clanwho") {
            desc = "List command clan-chat members"
            cheat { player.clanWho() }
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

    private fun Player.addFriend(name: String) {
        val target = findOnlinePlayer(name)
        if (target == null) {
            mes("Could not find an online player named '$name'.")
            return
        }
        if (target === this) {
            mes("You are already your own best contact here.")
            return
        }
        friendSet().add(target.chatKey())
        mes("${target.displayName} added to your session friends.", ChatType.FriendNotification)
        target.mes("${displayName} added you as a session friend.", ChatType.FriendNotification)
    }

    private fun Player.removeFriend(name: String) {
        val key = normalize(name)
        if (key.isBlank()) {
            mes("Usage: ::frienddel player")
            return
        }
        val removed = friendSet().remove(key)
        if (removed) {
            mes("Removed $name from your session friends.", ChatType.FriendNotification)
        } else {
            mes("$name is not in your session friends.")
        }
    }

    private fun Player.listFriends() {
        val set = friendSet()
        if (set.isEmpty()) {
            mes("Session friends: none. Add one with ::friendadd player.")
            return
        }
        val names =
            set.sorted().map { key ->
                val online = onlineByKey(key)
                online?.displayName ?: "$key (offline)"
            }
        mes("Session friends: ${names.joinToString(", ")}")
    }

    private fun Player.createClan(rawName: String) {
        val ownerKey = chatKey()
        if (clanByOwner.containsKey(ownerKey)) {
            mes("You already own a clan-chat. Use ::clanwho or ::clanleave.")
            return
        }
        leaveClan(silent = true)
        val name = rawName.ifBlank { "$displayName chat" }.take(MAX_CLAN_NAME_LENGTH)
        val clan = ClanChannel(name = name, ownerKey = ownerKey, members = mutableSetOf(ownerKey))
        clanByOwner[ownerKey] = clan
        clanByMember[ownerKey] = clan
        mes("Clan-chat created: $name. Others can use ::clanjoin $displayName.", ChatType.ClanChat)
    }

    private fun Player.joinClan(name: String) {
        val owner = findOnlinePlayer(name)
        val clan = owner?.let { clanByOwner[it.chatKey()] }
        if (clan == null) {
            mes("Could not find an online clan owner named '$name'.")
            return
        }
        leaveClan(silent = true)
        clan.members.add(chatKey())
        clanByMember[chatKey()] = clan
        clan.broadcast("${displayName} joined ${clan.name}.")
    }

    private fun Player.leaveClan(silent: Boolean = false) {
        val key = chatKey()
        val clan = clanByMember.remove(key)
        if (clan == null) {
            if (!silent) {
                mes("You are not in a clan-chat.")
            }
            return
        }
        clan.members.remove(key)
        if (clan.ownerKey == key) {
            clan.members.toList().forEach { clanByMember.remove(it) }
            clanByOwner.remove(key)
            if (!silent) {
                mes("Your clan-chat has been closed.", ChatType.ClanChat)
            }
            return
        }
        if (!silent) {
            mes("You leave ${clan.name}.", ChatType.ClanChat)
        }
        clan.broadcast("${displayName} left ${clan.name}.")
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

    private data class ClanChannel(
        val name: String,
        val ownerKey: String,
        val members: MutableSet<String>,
    )

    private companion object {
        const val MAX_MESSAGE_LENGTH = 120
        const val MAX_CLAN_NAME_LENGTH = 20
    }
}

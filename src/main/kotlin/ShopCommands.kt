import com.kotlindiscord.kord.extensions.DISCORD_BLURPLE
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.*
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.chatGroupCommand
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.reply
import dev.kord.rest.builder.message.create.allowedMentions

class ShopCommands : Extension() {
    override val name: String
        get() = "Shopping"


    class ViewArgs : Arguments() {
		val category by defaultingEnum(
			"category",
			"The items in a category (item/role/all)",
			defaultValue = Type.All,
			typeName = "Type"
		)
	}

    class BuyArgs : Arguments() {
        val itemId by string("ID", "The ID of them item you wanna buy")
    }

    class ShopAddRole : Arguments() {
        val role by role("role", "The role the members will buy")
        val price by defaultingInt("price", "The price to buy this role", 100)
        val isLimited by defaultingBoolean(
            "limited?",
            "Is the role purchasable just X times (by all users)",
            defaultValue = false
        )
        val limitCount by defaultingInt(
            "limit count",
            "If limited, how many copies of this item would you like to sell? (0 => unlimited)\nNOTE: You may **NOT** choose 0 if limited or any number if not limited",
            0
        )
        val description by defaultingCoalescingString(
            "description",
            "Describe about this role",
            "No description was provided"
        )
    }

    override suspend fun setup() {
        // Shop Commands
        chatGroupCommand(::ViewArgs) {
            name = "shop"
            aliasKey = "store"
            description = "Buy from, add to, remove from or look at this guild's shop"


            action {
                val type = Type.valueOf(arguments.category.name)
                println("Store type: $type")
                val ser = cache[guild?.id?.asString] ?: error("Cannot fetch guild ${guild?.id?.asString}")
                paginator(targetMessage = message, pingInReply = false) {
                    owner = message.author
                    keepEmbed = true
                    timeoutSeconds = 120


                    page {
                        title = "Shop"
                        description = "Showing items from this server's shop"
                        field("How to buy items?", true) {
                            """```md
                                    |1. Find the item you wanna buy
                                    |2. copy the item's ID
                                    |3. run ${ser.prefix}shop get  <id>```""".trimMargin()
                        }

                        color = message.getAuthorAsMember()?.color() ?: DISCORD_BLURPLE
                    }

                    if (type == Type.All && ser.shop.items.isNotEmpty()) {
                        ser.shop.items.forEach { item ->
                            page {
                                with(item) {
                                    title = "${type.name}: $name"
                                    this@page.description = "**ID: $id**"
                                    field("Name", true) { "```\n$name```" }
                                    field("Description", true) { "```\n$description```" }
                                    field("Price", true) { "```kt\n$price```" }
                                    field("Limited Amount?", true) { "```kt\n$isLimited```" }
                                    if (isLimited) field("Limit", true) { "```kt\n$limitCount```" }
                                }
                                color = message.getAuthorAsMember()?.color() ?: DISCORD_BLURPLE
                            }
                        }
                    } else if (ser.shop.items.any { it.type == type }) {
                        for (item in ser.shop.items.filter { it.type == type }) {
                            page {
                                with(item) {
                                    title = "${type.name}: $name"
                                    this@page.description = "**ID: $id**"
                                    field("Name", true) { "```\n$name```" }
                                    field("Description", true) { "```\n$description```" }
                                    field("Price", true) { "```kt\n$price```" }
                                    field("Limited Amount?", true) { "```kt\n$isLimited```" }
                                    if (isLimited) field("Limit", true) { "```kt\n$limitCount```" }
                                }
                                color = message.getAuthorAsMember()?.color() ?: DISCORD_BLURPLE
                            }
                        }
                    } else {
                        page {
                            title = "Shop"
                            description = "This guild has no items that are classified as ${type.type} in the shop"
                            color = message.getAuthorAsMember()?.color() ?: DISCORD_BLURPLE
                        }
                    }
                }.send()
            }

            // Add Commands
            chatGroupCommand {
                name = "add"
                description = "Add new items to the shop"

                check { failIf(!(event.message.getAuthorAsMember()?.isOwner() ?: false)) }
                // Role
                chatCommand(::ShopAddRole) {
                    name = "role"
                    description = "Add a role item to the shop"

                    action {
                        val ser = cache[guild?.id?.asString] ?: error("Cannot fetch guild ${guild?.id?.asString}")

                        message.reply {
                            allowedMentions()
                            with(arguments) {
								val item =
									Item(
										Type.Role,
										role.id.asString,
										role.name,
										description,
										price,
										isLimited,
										limitCount
									)
                                content = if (item !in ser.shop.items) {
                                    ser.shop.items.add(item)
                                    cache[ser.id] = ser
                                    database.write(ser.id, ser)
                                    "Role added successfully"
                                } else {
                                    "Role already exists in the store"
                                }
                            }
                        }
                    }
                }
            }

            // Remove
            chatCommand(::BuyArgs) {
                name = "remove"
                description = "Remove an item from the shop"

                check { failIf(!(event.message.getAuthorAsMember()?.isOwner() ?: false)) }
                action {
                    val ser = cache[guild?.id?.asString] ?: error("Cannot fetch guild ${guild?.id?.asString}")
                    val items = ser.shop.items

                    if (items.any { it.id == arguments.itemId }) {
                        items.removeIf { it.id == arguments.itemId }
                        message.reply {
                            allowedMentions()
                            content = "Successfully deleted item #${arguments.itemId}"
                        }
                        ser.shop.items.removeIf { it.id == arguments.itemId }
                        cache[ser.id] = ser
                        database.write(ser.id, ser)
                    } else {
                        message.reply {
                            allowedMentions()
                            content = "There's no such item with an ID of ${arguments.itemId}"
                        }
                    }
                }
            }

            // Buy
            chatCommand(::BuyArgs) {
                name = "get"
                aliasKey = "buy"
                description = "Get yourself something nice from the shop"

                action {
					val ser = cache[guild?.id?.asString] ?: error("Cannot fetch guild ${guild?.id?.asString}")
					val shop = ser.shop
					val itemId = arguments.itemId
					val customerId = message.getAuthorAsMember()!!.id.asString
					val customer = ser.community.find { it.id == customerId }
								   ?: error("Member $customerId wasn't found in ${guild!!.id.asString}")
					val item = shop.items.find { it.id == itemId }
	
					message.reply {
						allowedMentions()
						if (item == null) {
							content = "Item with an ID of $itemId could not be found"
						} else if (item.type == Type.Role && item in customer.items) {
							content = "You already own this item"
						} else if (customer.money < item.price) {
							content = "You don't have enough money to buy this item"
                        } else {
                            if (!item.isLimited) {
                                customer.items.add(item)
                                customer.money -= item.price
                            } else if (item.limitCount > 0) {
                                customer.items.add(item)
                                customer.money -= item.price
                                item.limitCount -= 1
                            }
                            content = "Enjoy your purchase :)"

                            if (item.type == Type.Role) {
                                val member = guild!!.getMember(Snowflake(customerId))
                                member.addRole(Snowflake(item.id), "Purchased from the store")
                            }
                        }
                    }
                }
            }
        }
    }
}
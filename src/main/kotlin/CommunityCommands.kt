import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingInt
import com.kotlindiscord.kord.extensions.commands.converters.impl.member
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalMember
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.chatCommand
import com.kotlindiscord.kord.extensions.utils.capitalizeWords
import dev.kord.common.Color
import dev.kord.core.behavior.reply
import dev.kord.rest.Image
import dev.kord.rest.builder.message.create.allowedMentions
import dev.kord.rest.builder.message.create.embed
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.random.Random

class CommunityCommands : Extension() {
	override val name: String
		get() = "Community Commands"
	/* Arguments */
	/**
	 * Arguments to get info about a member
	 * [member] - The member you'd like to get info about. Defaults to author
	 */
	class InfoArgs : Arguments() {
		val member by optionalMember("member", "The member you'd like to get info about")
	}
	
	/**
	 * Gift a member
	 *
	 * [member] -> The member to gift
	 *
	 * [amount] -> The amount of coins to give
	 */
	class GiftArgs : Arguments() {
		val mem by member("member", "The member you'd like to gift")
		val amount by defaultingInt("amount", "The amount of coins you'd like to gift", 100)
	}
	
	override suspend fun setup() {
		/* Chat Commands */
		// Info
		chatCommand(::InfoArgs)
		{
			name = "info"
			description = "Get info about a user"
			
			action {
				val target = arguments.member ?: message.getAuthorAsMember()!!
				val ser = cache[guild?.id?.asString] ?: error("Not a valid guild")
				val mem = ser.community.firstOrNull { it.id == target.id.asString }
				message.reply {
					allowedMentions()
					if (mem == null) {
						content = "This user isn't a part of the community (probably a city hall worker)"
					} else {
						embed {
							thumbnail { url = target.avatar.url }
							author { name = "ID: ${target.id.asString}" }
							title = "[INFO] ${target.displayName}"
							with(mem) {
								val decimalFormat = DecimalFormat("000")
								field("Bank", true) { "```\n$money \u235F```" }
								field("\u200b", true) { "\u200b" }
								field("Items Count", true) { "```\n${decimalFormat.format(items.size)}```" }
								field("Item List", true) {
									"""```css
                                |${items.joinToString("\n")}${"\u200b"}
                                |```""".trimMargin()
								}
							}
							color = target.color()
						}
					}
				}
			}
		}
		// Avatar
		chatCommand(::InfoArgs)
		{
			name = "avatar"
			description = "Get a user's avatar"
			aliasKey = "pfp"
			
			action {
				val target = arguments.member ?: message.getAuthorAsMember()!!
				message.reply {
					allowedMentions()
					content = target.avatar.getUrl(Image.Size.Size4096)
				}
			}
		}
		// Work
		chatCommand {
			name = "work"
			description = "Work to earn some money"
			
			action {
				val ser = cache[guild?.id?.asString] ?: error("Not a valid guild")
				val cmember = ser.community.filter { it.id == message.author!!.id.asString }[0]
				with(cmember) {
					if (lastWork == null || ChronoUnit.HOURS.between(lastWork!!, LocalDateTime.now()) >= 4) {
						val amount = Random.nextInt(100, 1000)
						money += amount
						message.reply {
							allowedMentions()
							embed {
								title = "Salary"
								field("Gained", true) { "```\n$amount ⍟```" }
								field("Bank", true) { "```\n${money} ⍟```" }
								field("Next Work", !true) { "Your next work will be available in 4 hours" }
								
								color = Color(50, 205, 50)
							}
						}
						lastWork = LocalDateTime.now().toInstant(ZoneOffset.UTC)
					} else {
						var minutesLeft = ChronoUnit.MINUTES.between(
							LocalDateTime.now().toInstant(ZoneOffset.UTC),
							lastWork!!.plus(4, ChronoUnit.HOURS)
						)
						val hoursLeft = minutesLeft / 60
						minutesLeft -= hoursLeft * 60
						message.reply {
							allowedMentions()
							embed {
								title = "ERROR"
								field("Cause", true) { "You're on a cooldown" }
								field("Time Left", true) { "$hoursLeft hours and $minutesLeft minutes" }
								color = Color(java.awt.Color.RED.rgb)
							}
						}
					}
				}
				database.write(ser.id, ser)
			}
		}
		// Gift
		chatCommand(::GiftArgs) {
			name = "gift"
			aliases = arrayOf("give", "donate")
			description = "Gift a member a bit of your money"
			
			action {
				val ser = cache[guild?.id?.asString] ?: error("Cannot fetch guild ${guild?.id?.asString}")
				val gifted = arguments.mem
				val amount = arguments.amount
				val gifter = ser.community.find {
					it.id == message.author!!.id.asString
				} ?: error(
					"Cannot find user: ${message.getAuthorAsMember()!!.id.asString}" +
					" in guild ${ser.id}"
				)
				
				if (amount > gifter.money) {
					message.reply {
						allowedMentions()
						content = "You can't give more money than u have [${gifter.money}]"
					}
				} else if (gifted.id.asString == gifter.id) {
					message.reply {
						allowedMentions()
						content = "You can't gift yourself (oh and you lost $amount ⍟)"
					}
					val index = ser.community.indexOf(gifter)
					ser.community[index].money -= amount
				} else {
					val giftedMember = ser.community.find { it.id == gifted.id.asString }
									   ?: error("Cannot find user: ${gifted.id.asString} in guild ${ser.id}")
					val gifterIndex = ser.community.indexOf(gifter)
					val giftedIndex = ser.community.indexOf(giftedMember)
					ser.community[giftedIndex].money += amount
					ser.community[gifterIndex].money -= amount
					message.reply {
						allowedMentions()
						embed {
							title = "Your gift has been sent".title()
							("${gifted.mention} you were gifted $amount ⍟ by " +
							 message.getAuthorAsMember()!!.mention).also { description = it }
							color = Color(50, 205, 50)
						}
					}
				}
				cache[ser.id] = ser
				database.write(ser.id, ser)
			}
		}
	}
}

private fun String.title() = capitalizeWords()
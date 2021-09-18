import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalMember
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.chatCommand
import com.kotlindiscord.kord.extensions.extensions.publicUserCommand
import com.kotlindiscord.kord.extensions.interactions.respond
import dev.kord.core.behavior.reply
import dev.kord.rest.builder.message.create.allowedMentions
import org.litote.kmongo.eq
import org.litote.kmongo.findOne

class Tests : Extension() {
	override val name: String
		get() = "Test Commands"
	
	class InfoArguments : Arguments() {
		val member by optionalMember("member", "The member you'd like to check")
	}
	
	override suspend fun setup() {
		publicUserCommand {
			name = "greet"
			
			
			action {
				val target = targetUsers.toList()[0].asMember(guild?.id ?: error("Invalid guildId"))
				
				respond {
					allowedMentions {
						users.add(target.id)
					}
					content = "${target.mention} you were greeted by ${user.mention}"
				}
			}
		}
		
		chatCommand(::InfoArguments) {
			name = "check"
			
			check { failIf(event.message.author?.id?.asString != "279879920040148992") }
			action {
				val ser = database.findOne { Server::id eq guild?.id?.asString } ?: error("OOp")
				val target = arguments.member ?: message.getAuthorAsMember()!!
				val mem = ser.community.first { it.id == target.id.asString }
				
				message.reply {
					content = mem.lastWork.toString()
				}
			}
		}
	}
}
/*
 * ---------------!!! ADD TO README !!!---------------
 * This module houses the registration process for point accumulation.
 * Members have to be registered in order to get points since, by default,
 * they will earn a point everytime they send a message on the server.
 * If we let all members to earn points without registering, the bot can get
 * overloaded with requests since it has to connect with the database twice
 * each time a member earns a point.
 * 
 * Available commands are as follows:
 * -	$register <password>
 * 		Registers the member to the database. A password is required for later use.
 * 
 * -	$deregister <password>
 * 		Removes the member from the database and will no longer earn points.
 */
package commands;

import java.sql.SQLException;
import javax.security.auth.login.LoginException;
import bot_init.LazyJavie;
import bot_init.SQLconnector;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class Register extends ListenerAdapter{	
	public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
		String[] args = event.getMessage().getContentRaw().split("\\s+");
		String masterPass = "masterPassword6969";
		String msg = event.getMessage().getContentRaw();
		
		//-------------------------REGISTER-------------------------
		if (args[0].equalsIgnoreCase(LazyJavie.prefix + "register")) {
			
			//Initialization
			String requestby = event.getMember().getUser().getName();
			P.print("\nRegistration request: " + event.getMember().getUser());
			
			//<REGISTER: PASSWORD WITH SPACE>
			if (args.length > 2) {
				//Embed block
				EmbedBuilder noArgsRegister = new EmbedBuilder();
				noArgsRegister.setColor(0x77B255);
				noArgsRegister.addField("Please don't use spaces for your password", "```" + LazyJavie.prefix + "register <password>```.",true);
				event.getChannel().sendMessage(noArgsRegister.build()).queue();				
				return;
			}
			try {
				//Initialization
				String password = args[1];
				
				//Checks if the member is already registered.
				try {
					P.print("Attempting registration...");
					String s = SQLconnector.get("select * from lazyjavie.members where userid = '" + event.getMember().getId() + "'", "userid", false);
					
					//<MEMBER ALREADY REGISTERED>
					if (s != "Empty result.") {
						P.print("Member found. Cancelling. " + s);
						EmbedBuilder alreadyRegistered = new EmbedBuilder();
						alreadyRegistered.setColor(0x77B255);
						alreadyRegistered.addField("You are already registered","If you wish to deregister: ```" + LazyJavie.prefix + "deregister``` :warning: This will reset your points.",true);
						event.getChannel().sendMessage(alreadyRegistered.build()).queue();
						return;
					}
				}
				catch (LoginException e) {P.print("Error encountered: " + e.toString()); SQLconnector.callError(msg, e.toString()); return;}
				catch (SQLException e) {P.print("Error encountered: " + e.toString()); SQLconnector.callError(msg, e.toString()); return;}
				catch (Exception e) {P.print("Error encountered: " + e.toString()); SQLconnector.callError(msg, e.toString()); return;}
				
				//<REGISTER: SUCCESS>
				P.print("Registering...");
				SQLconnector.update("insert into lazyjavie.members (userid, userpass) values (\"" + event.getMember().getId() + "\", \"" + password + "\")",false);
				
				//EmbedBlock
				EmbedBuilder successRegister = new EmbedBuilder();
				successRegister.setColor(0x77B255);
				successRegister.setTitle("You have successfully been registered, " + event.getMember().getUser().getName() + "!");
				successRegister.setFooter("Requested by " + requestby , event.getMember().getUser().getAvatarUrl());
				event.getChannel().sendMessage(successRegister.build()).queue();
				P.print("Registered member: " + event.getMember().getUser() + "");
			}
			catch (LoginException e) {P.print("Error encountered: " + e.toString()); SQLconnector.callError(msg, e.toString()); return;}
			catch (SQLException e) {P.print("Error encountered: " + e.toString()); SQLconnector.callError(msg, e.toString()); return;}
			catch (ArrayIndexOutOfBoundsException e) {
				P.print("Cancelling; missing argument.");
				//Embed block
				EmbedBuilder noArgsRegister = new EmbedBuilder();
				noArgsRegister.setColor(0x77B255);
				noArgsRegister.addField("Please enter an argument.", "```" + LazyJavie.prefix + "register <password>```",true);
				event.getChannel().sendMessage(noArgsRegister.build()).queue();
				SQLconnector.callError(msg, "[REGISTER] NO ARGS");
				return;
			}
			catch (Exception e) {
				//Embed block
				EmbedBuilder errorEncountered = new EmbedBuilder();
				errorEncountered.setColor(0x77B255);
				errorEncountered.addField("Error encountered: " + e, "type ```" + LazyJavie.prefix + "register <password>",true);
				event.getChannel().sendMessage(errorEncountered.build()).queue();
				SQLconnector.callError(msg, e.toString());
				return;
			}
		}
		
		//-------------------------DEREGISTER-------------------------
		else if (args[0].equalsIgnoreCase(LazyJavie.prefix + "deregister")) {
			
			//Initialization
			String requestby = event.getMember().getUser().getName();
			P.print("\nDeregistration request: " + event.getMember().getUser().getName());
			
			//Gets the member's password.
			P.print("Getting the member's password...");
			String pass = null;
			try {pass = SQLconnector.get("select * from lazyjavie.members where userid ='" + event.getMember().getId() + "'", "userpass", false);}
			catch (LoginException e) {P.print(e.toString()); SQLconnector.callError(msg, e.toString()); return;}
			catch (SQLException e ) {P.print(e.toString()); SQLconnector.callError(msg, e.toString()); return;}
			catch (Exception e) {P.print(e.toString()); SQLconnector.callError(msg, e.toString()); return;}
			
			//<DEREGISTER: MISSING ARGS> Checks if there are missing arguments.
			P.print("Checking for invalid arguments...");
			if(args.length < 2) {
				P.print("Cancelling; missing argument.");
				EmbedBuilder deregister = new EmbedBuilder();
				deregister.setColor(0xD82D42);
				deregister.setTitle("Are you sure you want to deregister?");
				deregister.setDescription("Enter `"+ LazyJavie.prefix + "deregister <password>` to confirm.");			
				deregister.setFooter("Requested by " + requestby , event.getMember().getUser().getAvatarUrl());
				event.getChannel().sendMessage(deregister.build()).queue();
				return;
			}
			
			//<DEREGISTER: NOT REGISTERED> Checks if the member isn't already registered.
			//<DEREGISTER: SUCCESS> Deregisters the member.
			else if(args[1].equals(pass) || args[1].equals(masterPass)) {
				P.print("Deregistering...");
				try {
					SQLconnector.update("DELETE FROM lazyjavie.members WHERE userid=" + event.getMember().getId(), false);
				}
				catch (LoginException e) {P.print(e.toString()); SQLconnector.callError(msg, e.toString()); return;}
				catch (SQLException e) {P.print(e.toString()); SQLconnector.callError(msg, e.toString()); return;}
				catch (Exception e) {P.print(e.toString()); SQLconnector.callError(msg, e.toString()); return;}
				
				P.print("Successfully deregistered " + event.getMember().getUser().getName());
				if (args[1].equals(masterPass)) event.getMessage().delete().queue();
				//Embed block
				EmbedBuilder successDeregister = new EmbedBuilder();
				successDeregister.setColor(0x77B255);
				successDeregister.setTitle("Successfully deleted the user: `" + event.getMember().getUser() + "` from the database.");			
				successDeregister.setFooter("Requested by " + requestby , event.getMember().getUser().getAvatarUrl());
				event.getChannel().sendMessage(successDeregister.build()).queue();
			}
			
			//<UNREGISER: INVALID PASSWORD> When the user enters an incorrect password.
			else {
				P.print("Invalid password: " + event.getMessage().getContentRaw());
				EmbedBuilder incorrectPassword = new EmbedBuilder();
				incorrectPassword.setColor(0xD82D42);
				incorrectPassword.addField(":x: Incorrect password!", "Enter `"+ LazyJavie.prefix + "deregister <password>` to confirm.", true);
				incorrectPassword.setFooter("Requested by " + requestby , event.getMember().getUser().getAvatarUrl());
				event.getChannel().sendMessage(incorrectPassword.build()).queue();
			}
		}
	}
}

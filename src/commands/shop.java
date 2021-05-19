package commands;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.security.auth.login.LoginException;

import bot_init.LazyJavie;
import bot_init.SQLconnector;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class shop extends ListenerAdapter {
	
	public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
		String[] args = event.getMessage().getContentRaw().split("\\s+");
	    
		if (args[0].equalsIgnoreCase(LazyJavie.prefix + "shop")) {
			P.print("\n[shop] Shop query by: " + event.getMember().getUser().getName());
			
			//Initialization
			String requestby = event.getMember().getUser().getName();
			List<Role> roles = event.getGuild().getRoles();
			P.print("Getting list of blacklisted roles...");
	    	List<String> blacklist = null;
		    StringBuilder displayRoles = new StringBuilder();
		    try {blacklist = SQLconnector.getList("select * from lazyjavie.roleblacklist", "rolename", true);}
		    catch (LoginException e) {P.print("Error encountered: " + e.toString());}
		    catch (SQLException e) {P.print("Error encountered: " + e.toString());}

			//<SHOP: EMPTY LIST>Checks if there are no roles in the server.
			if (roles.isEmpty()) {
				EmbedBuilder shopRoles = new EmbedBuilder();
				shopRoles.setColor(0xffae00);
				shopRoles.setTitle("There are no roles in this guild!");
				event.getChannel().sendMessage(shopRoles.build()).queue();
				return;
		    }
			
			//-------------------------Blacklist Block starts here.-------------------------
		    //Checks for blacklisted roles; only includes non-blacklisted roles.
		    P.print("Removing blacklisted roles from output..."); 
		    for (Role r : roles) {
		    	String roleName = r.getName().toString();
		    	
		    	//TODO (Optional) Move this to database
		    	boolean[] permsArray = {
	    			r.hasPermission(Permission.ADMINISTRATOR),
	    			r.hasPermission(Permission.KICK_MEMBERS),
	    			r.hasPermission(Permission.BAN_MEMBERS),
	    			r.hasPermission(Permission.MANAGE_SERVER),
	    			r.hasPermission(Permission.MANAGE_CHANNEL),
	    			r.hasPermission(Permission.MANAGE_PERMISSIONS),
	    			r.hasPermission(Permission.MESSAGE_MANAGE),
	    			r.hasPermission(Permission.MANAGE_EMOTES),
	    			r.hasPermission(Permission.MANAGE_WEBHOOKS)
			    };
		    	
		    	//Adds the array to a list.
			    ArrayList<Boolean> perms = new ArrayList<Boolean>();
			    for (boolean item : permsArray) {perms.add(item);}
		    	
			    //Checks if the player has the currently iterated role.
			    if (event.getMember().getRoles().contains(r)) {P.print("Blacklisted " + r.getName() + " (Already claimed)");}
			    
		    	//Any role colored #696969 (Hex); filters out bots.
			    else if(r.getColorRaw() == 0x696969) {}
		    	
		    	//Any role with the permission
		    	else if (perms.contains(true)) {P.print("Removed " + r.getName());}
		    	
		    	//Any role part of the blacklist.
		    	else if (blacklist.contains(r.getName().toLowerCase())) {P.print("Blacklisted " + r.getName());}
		    	
		    	else {
		    		//Gets the role's price.
		    		String stringRolePrice = null;
		    		try {
		    		P.print("Getting role price...");
			    	stringRolePrice = SQLconnector.get("SELECT * FROM lazyjavie.sellroles WHERE roleName='"+ r.getName() +"'", "rolePrice", false);
		    		} catch (LoginException e) {P.print("Error encountered: " + e.toString());}
		    		catch (SQLException e) {P.print("Error encountered: " + e.toString());}
			    	//Adds the role to the displayed list.
		    		displayRoles.append("� **" + r.getName() + ":** `" + stringRolePrice + " points`").append("\n");
		    		
		    		//Adds the role to the table.
					try {
						P.print("Adding role to table...");
						SQLconnector.update("INSERT INTO lazyjavie.shop (itemname, price) VALUES ('" + roleName + "', 0);", true);
					} catch (SQLException e) {
						if (e.toString().contains("Duplicate entry")) {P.print("Failed: Role already exists in table.");}}
					catch (Exception e) {e.printStackTrace();}
					//P.print(SQLconnector.getList("select * from lazyjavie.sellroles", "roleName").toString());
		    	}	
		    }	//-------------------------Blacklist block ends here.-------------------------
		    P.print("Blacklist check done.");
		    
		    //Displays the items available for purchase.
			if (args.length == 1 && args[0] == LazyJavie.prefix + "shop"){
				//Embed block
				EmbedBuilder shop = new EmbedBuilder();
				shop.setColor(0xffae00);
				shop.setTitle(":moneybag: Welcome to the LazyJavie shop! :moneybag:");
				shop.setDescription("To purchase a role, type `" + LazyJavie.prefix + "shop buy [role]`" + "\r\n" + "");
			    event.getGuild().getRolesByName("bots", true);
				shop.addField("List of Available Roles:","" + displayRoles , true);
	     		shop.setFooter("Requested by " + requestby , event.getMember().getUser().getAvatarUrl());
				event.getChannel().sendMessage(shop.build()).queue();
				return;
			}
			
			//<BUY: NO NAME> For when a member attempts to buy a role but doesn't enter a name.
			if(args.length == 2 && args[1] == "buy") {
				//Embed block
				EmbedBuilder buyRole = new EmbedBuilder();
				buyRole.setColor(0xffae00);
				buyRole.setTitle("You didn't enter a role to buy");
				buyRole.setDescription("To purchase a role, type `" + LazyJavie.prefix + "shop buy [role]`" + "\r\n" + "");
				buyRole.addField("List of Available Roles:", "" + displayRoles , true);
			    buyRole.setFooter("Requested by " + requestby , event.getMember().getUser().getAvatarUrl());
				event.getChannel().sendMessage(buyRole.build()).queue();
				return;
			}
			//-------------------------[BUY] A successful attempt at purchasing.-------------------------
			if (args[1].equalsIgnoreCase("buy") && args.length >= 3) {
				P.print("[shop] Buy request by: " + event.getMember().getUser().getName());
				try {
					P.print("Checking through roles...");
				    for (Role r : roles) {
						if(args[2].equals(r.getName())) {
							
							//Initialization. Gets the role's price.
							String memberId = event.getMessage().getMember().getId();
							Integer pts = Integer.parseInt(SQLconnector.get("select points from lazyjavie.members WHERE userid=" + memberId + ";", "points", true));
							String stringRolePrice = "2147483647";	//Redundancy, in case the price isn't properly defined.
							Integer intRolePrice = null;
							try {stringRolePrice = SQLconnector.get("SELECT * FROM lazyjavie.shop WHERE itemname='" +r.getName()+ "'", "price", true);}
							catch (Exception e) {
								P.print("Error encountered: " + e.toString());
								event.getChannel().sendMessage("Error ecountered: `" + e.toString() + "`\nPlease contact an admin to set a price.");
							}
							finally {intRolePrice = Integer.parseInt(stringRolePrice);}
							
							//<BUY: NO PRICE> Checks if there is no set price.
							if (stringRolePrice == null) {
								P.print("Purchase cancelled: No price set.");
								EmbedBuilder noPrice = new EmbedBuilder();
								noPrice.setColor(0xD82D42);
								noPrice.addField("There is no set price for that role!","Current points: `" + pts + "`",true);
								noPrice.setFooter("Requested by " + requestby , event.getMember().getUser().getAvatarUrl());
							    event.getChannel().sendMessage(noPrice.build()).queue();
							    return;
					        }
							
					        //<BUY: SUCCESS> Successful purchase.
					        else if (intRolePrice < pts && args[2].equalsIgnoreCase(r.getName()) && !blacklist.contains(args[2].toLowerCase())) {
				        		
					        	P.print("Filter checked.");
					        	//Embed block
								EmbedBuilder purchaseComplete = new EmbedBuilder();
								purchaseComplete.setColor(0xD82D42);
								purchaseComplete.addField("You have purchased the role: ", "`" + r.getName() + "`", true);
								event.getChannel().sendMessage(purchaseComplete.build()).queue();
								
								//Initialization
								Member member = event.getMember();
								Role role = event.getGuild().getRoleById(r.getId());
								
								P.print("Deducting points...");
								//Points deduction
								pts -= intRolePrice;
								SQLconnector.update("UPDATE lazyjavie.members "+ "SET points = " + pts + " WHERE userid=" + memberId + ";", true);
								
								P.print("Applying role...");
								//Role application
								event.getGuild().addRoleToMember(member, role).queue();;
								event.getGuild().modifyMemberRoles(member, role).queue();;
								P.print("Purchase successful.");
								return;
					        }	
							//<BUY: INSUFFICIENT FUNDS> Not enough funds.
					        else if (intRolePrice > pts  && args[2].equalsIgnoreCase(r.getName()) && !blacklist.contains(args[2].toLowerCase())) {
					        	P.print("Purchase cancelled: Not enough points.");
					        	EmbedBuilder noMoney = new EmbedBuilder();
					        	noMoney.setColor(0xD82D42);
					        	noMoney.addField("You don't have enough points", "Current points: `" + pts + "`", true);
							    noMoney.setFooter("Requested by " + requestby , event.getMember().getUser().getAvatarUrl());
								event.getChannel().sendMessage(noMoney.build()).queue();
								return;
					        }
				    	}
				    } 
			    } catch (Exception e) {P.print("Error encountered: " + e); e.printStackTrace();}
			} 
		}
	}
}
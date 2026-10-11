package com.minecraftmode.client.menu;

import com.minecraftmode.client.ClientSettings;
import com.minecraftmode.client.raid.RaidClient;
import com.minecraftmode.economy.Wallet;
import com.minecraftmode.job.JobClass;
import com.minecraftmode.loot.Coins;
import com.minecraftmode.network.MenuActionPayload;
import com.minecraftmode.network.PartySyncPayload;
import com.minecraftmode.raid.Parties;
import java.util.List;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/**
 * The adventurer's menu (semicolon, or the pause menu): the party ({@code /party}), paying another player ({@code /wallet pay}) and
 * this client's display settings ({@code /damagenumbers}, {@code /enhanceeffects}, {@code /hpbar}). The party part follows
 * {@link RaidClient#party()}, which the server syncs twice a second.
 */
public class AdventurerScreen extends MenuScreenBase {
	static final String K = "screen.minecraft_mode.menu.";
	private static final int W = 320;
	private static final int H = 230;
	private static final int PARTY = 0;
	private static final int PAY = 1;
	private static final int NAMES_PER_PAGE = 8;

	private List<String> shownParty = List.of();
	private String shownInvite = "";
	private int shownBalance;
	private int namesPage;
	private String payee = "";

	public AdventurerScreen() {
		super(Component.translatable(K + "title"), W, H);
	}

	@Override
	protected List<Component> tabs() {
		return List.of(Component.translatable(K + "party"), Component.translatable(K + "pay"), Component.translatable(K + "settings"));
	}

	@Override
	protected void initTab(final int x, final int y) {
		this.shownParty = partyKey();
		this.shownInvite = RaidClient.invitedBy();
		this.shownBalance = Wallet.balance(this.minecraft.player);
		switch (this.tab) {
			case PARTY -> this.party(x, y);
			case PAY -> this.pay(x, y);
			default -> this.settings(x, y);
		}
	}

	@Override
	public void tick() {
		super.tick();
		// what this screen shows of the party (health changes every sync in a fight and is not shown); payments change the balance
		if (!partyKey().equals(this.shownParty) || !RaidClient.invitedBy().equals(this.shownInvite)
			|| this.minecraft.player != null && Wallet.balance(this.minecraft.player) != this.shownBalance) {
			this.rebuildWidgets();
		}
	}

	private static List<String> partyKey() {
		return RaidClient.party().stream().map(m -> m.name() + m.online() + m.level() + m.job() + m.tier()).toList();
	}

	// ------------------------------------------------------------------ party

	private void party(final int x, final int y) {
		LocalPlayer self = this.minecraft.player;
		List<PartySyncPayload.Member> party = RaidClient.party();
		boolean leader = party.isEmpty() || party.getFirst().id().equals(self.getUUID());
		int rows = y;
		if (party.isEmpty()) {
			this.label(x, rows + 3, Component.translatable(K + "no_party").withStyle(ChatFormatting.GRAY));
			this.button(x + 150, rows, 90, Component.translatable(K + "create"), () -> send(MenuActionPayload.PARTY_CREATE, "", 0));
			rows += ROW;
			if (!this.shownInvite.isEmpty()) {
				this.label(x, rows + 3, Component.translatable(K + "invited", this.shownInvite).withStyle(ChatFormatting.GOLD));
				this.button(x + 150, rows, 90, Component.translatable(K + "accept").withStyle(ChatFormatting.GREEN), () -> send(MenuActionPayload.PARTY_ACCEPT, "", 0));
				rows += ROW;
			}
		} else {
			this.heading(x, rows, Component.translatable(K + "members", party.size(), Parties.MAX_SIZE));
			rows += 12;
			for (int i = 0; i < party.size(); i++) {
				PartySyncPayload.Member m = party.get(i);
				JobClass job = JobClass.values()[Math.floorMod(m.job(), JobClass.values().length)];
				Component line = Component.literal((i == 0 ? "★ " : "  ") + m.name()).withStyle(m.online() ? ChatFormatting.WHITE : ChatFormatting.DARK_GRAY);
				if (m.online()) {
					line = line.copy().append(Component.literal("  Lv." + m.level() + " ").withStyle(ChatFormatting.GRAY))
						.append(Component.translatable(job.tierKey(m.tier())).withColor(job.color()));
				}
				this.label(x, rows + 3, line);
				if (leader && i > 0) {
					this.button(x + 200, rows, 48, Component.translatable(K + "promote"), () -> send(MenuActionPayload.PARTY_PROMOTE, m.name(), 0));
					this.confirm(x + 250, rows, 48, "kick:" + m.name(), Component.translatable(K + "kick"), () -> send(MenuActionPayload.PARTY_KICK, m.name(), 0));
				}
				rows += ROW;
			}
			this.confirm(x, rows + 2, 70, "leave", Component.translatable(K + "leave"), () -> send(MenuActionPayload.PARTY_LEAVE, "", 0));
			if (leader) {
				this.confirm(x + 74, rows + 2, 70, "disband", Component.translatable(K + "disband"), () -> send(MenuActionPayload.PARTY_DISBAND, "", 0));
			}
			rows += ROW + 4;
		}
		if (leader && party.size() < Parties.MAX_SIZE) {
			this.heading(x, rows + 4, Component.translatable(K + "invite"));
			List<String> names = MenuClient.onlineNames(this.minecraft).stream()
				.filter(n -> party.stream().noneMatch(m -> m.name().equals(n)))
				.toList();
			this.names(x, rows + 16, names, name -> send(MenuActionPayload.PARTY_INVITE, name, 0), "");
		}
	}

	// ------------------------------------------------------------------ pay

	private void pay(final int x, final int y) {
		int balance = Wallet.balance(this.minecraft.player);
		this.label(x, y + 3, Component.translatable(K + "balance").append(" ").append(Coins.component(balance)));
		this.heading(x, y + 18, Component.translatable(K + "payee"));
		this.names(x, y + 30, MenuClient.onlineNames(this.minecraft), name -> {
			this.payee = name;
			this.rebuildWidgets();
		}, this.payee);
		int ay = y + 30 + (NAMES_PER_PAGE / 2) * ROW + 22;
		this.heading(x, ay, Component.translatable(K + "amount"));
		this.numberBox(x, ay + 12, 50, "gold", Component.translatable(K + "gold"));
		this.numberBox(x + 54, ay + 12, 50, "silver", Component.translatable(K + "silver"));
		this.numberBox(x + 108, ay + 12, 50, "copper", Component.translatable(K + "copper"));
		Component to = this.payee.isEmpty() ? Component.translatable(K + "pick_payee") : Component.translatable(K + "send", this.payee);
		this.confirm(x + 166, ay + 12, 130, "pay", to, () -> {
			long copper = (long)Math.max(0, this.number("gold", 0)) * Coins.GOLD + (long)Math.max(0, this.number("silver", 0)) * Coins.SILVER
				+ Math.max(0, this.number("copper", 0));
			if (copper > 0 && copper <= Integer.MAX_VALUE) {
				send(MenuActionPayload.PAY, this.payee, (int)copper);
			}
		}).active = !this.payee.isEmpty();
	}

	// ------------------------------------------------------------------ settings

	private void settings(final int x, final int y) {
		this.toggle(x, y, "damage_numbers", ClientSettings.damageNumbers, ClientSettings::setDamageNumbers);
		this.toggle(x, y + ROW + 4, "enhance_effects", ClientSettings.enhanceEffects, ClientSettings::setEnhanceEffects);
		this.toggle(x, y + 2 * (ROW + 4), "hp_bar", ClientSettings.hpBar, ClientSettings::setHpBar);
	}

	private void toggle(final int x, final int y, final String key, final boolean on, final Consumer<Boolean> set) {
		this.label(x, y + 3, Component.translatable(K + "setting." + key));
		Component state = Component.translatable(on ? "options.on" : "options.off").withStyle(on ? ChatFormatting.GREEN : ChatFormatting.GRAY);
		this.button(x + 200, y, 80, state, Component.translatable(K + "setting." + key + ".hint"), () -> {
			set.accept(!on);
			this.rebuildWidgets();
		});
	}

	// ------------------------------------------------------------------ shared

	/** Online player names as buttons in two columns, a page at a time; {@code selected} is shown pressed. */
	private void names(final int x, final int y, final List<String> names, final Consumer<String> pick, final String selected) {
		if (names.isEmpty()) {
			this.label(x, y + 3, Component.translatable(K + "nobody").withStyle(ChatFormatting.GRAY));
			return;
		}
		int pages = (names.size() + NAMES_PER_PAGE - 1) / NAMES_PER_PAGE;
		this.namesPage = Math.min(this.namesPage, pages - 1);
		int from = this.namesPage * NAMES_PER_PAGE;
		for (int i = from; i < Math.min(names.size(), from + NAMES_PER_PAGE); i++) {
			String name = names.get(i);
			int slot = i - from;
			this.button(x + (slot % 2) * 140, y + (slot / 2) * ROW, 136, Component.literal(name), () -> pick.accept(name)).active = !name.equals(selected);
		}
		if (pages > 1) {
			this.pager(x + 290 - 66, y + (NAMES_PER_PAGE / 2) * ROW + 2, this.namesPage, pages, page -> {
				this.namesPage = page;
				this.rebuildWidgets();
			});
		}
	}

	private static void send(final int action, final String name, final int amount) {
		if (ClientPlayNetworking.canSend(MenuActionPayload.TYPE)) {
			ClientPlayNetworking.send(new MenuActionPayload(action, name, amount));
		}
	}
}

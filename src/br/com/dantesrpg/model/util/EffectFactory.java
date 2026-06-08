package br.com.dantesrpg.model.util;

import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.enums.TipoEfeito;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashMap;

public class EffectFactory {

	private static final Map<String, EffectCreator> presets = new LinkedHashMap<>();

	// Intervalo padrão para ticks de DoT (em TU)
	private static final int TICK_INTERVAL = 100;

	static {
		// =====================================================================
		// EFEITOS RÁPIDOS (Pouca duração)
		// =====================================================================

		// Toxina (Grau 2): 4 ticks, 50% do dano por tick | Intervalo 50 TU | Duração 200 TU
		presets.put("Toxina", (duracao, danoDaSource) -> {
			int danoPorTick = Math.max(1, (int) (danoDaSource * 0.50));
			return new Efeito("Toxina", TipoEfeito.DOT, 200, null, danoPorTick, 50);
		});

		// Choque (Grau 1): Apenas +20 TU ao alvo no momento da aplicação (não é DoT)
		presets.put("Choque", (duracao, danoDaSource) ->
			new Efeito("Choque", TipoEfeito.DEBUFF, 100, null, 0, 0)
		);

		// Pesadelo (Grau 2): Só pode ser aplicado em alvos "Dormindo". Acorda e +60% dano recebido.
		presets.put("Pesadelo", (duracao, danoDaSource) ->
			new Efeito("Pesadelo", TipoEfeito.DEBUFF, 200, null, 0, 0)
		);

		// =====================================================================
		// EFEITOS MEDIANOS
		// =====================================================================

		// Sono (Grau 1): Aplica acúmulos (sem tempo limite). 5 acúmulos = Dormindo (300 TU, acorda com 2 ticks de dano)
		presets.put("Sono", (duracao, danoDaSource) ->
			new Efeito("Sono", TipoEfeito.DEBUFF, 99999, null, 0, 0)
		);

		// Sangramento (Grau 1): 5 ticks, 25% do dano por tick | Intervalo 100 TU | Duração 500 TU
		presets.put("Sangramento", (duracao, danoDaSource) -> {
			int danoPorTick = Math.max(1, (int) (danoDaSource * 0.25));
			return new Efeito("Sangramento", TipoEfeito.DOT, 500, null, danoPorTick, TICK_INTERVAL);
		});

		// Veneno (Grau 1): 6 ticks, 20% do dano por tick | Intervalo 100 TU | Duração 600 TU
		presets.put("Veneno", (duracao, danoDaSource) -> {
			int danoPorTick = Math.max(1, (int) (danoDaSource * 0.20));
			return new Efeito("Veneno", TipoEfeito.DOT, 600, null, danoPorTick, TICK_INTERVAL);
		});

		// =====================================================================
		// EFEITOS LONGOS
		// =====================================================================

		// Queimação (Grau 1): 6 ticks, 30% do dano por tick | Intervalo 100 TU | Duração 600 TU
		presets.put("Queimação", (duracao, danoDaSource) -> {
			int danoPorTick = Math.max(1, (int) (danoDaSource * 0.30));
			return new Efeito("Queimação", TipoEfeito.DOT, 600, null, danoPorTick, TICK_INTERVAL);
		});

		// HellFire (Grau 3): 20 ticks, 15% do dano por tick | -2% cura por tick | Duração 2000 TU
		presets.put("HellFire", (duracao, danoDaSource) -> {
			int danoPorTick = Math.max(1, (int) (danoDaSource * 0.15));
			Map<String, Double> mods = new HashMap<>();
			mods.put("REDUCAO_CURA", 0.0); // Começa em 0%, incrementa +2% a cada tick no CombatManager
			return new Efeito("HellFire", TipoEfeito.DOT, 2000, mods, danoPorTick, TICK_INTERVAL);
		});

		// Hemorragia (Grau 2): 5 ticks, 2% da vida máxima por tick | Reduz cura em 30% pelo dobro da duração
		// Duração DoT: 500 TU | Redução de cura: 1000 TU (aplicada separadamente)
		presets.put("Hemorragia", (duracao, danoDaSource) -> {
			Map<String, Double> mods = new HashMap<>();
			mods.put("PERCENTUAL_HP_MAX", 0.02);
			mods.put("REDUCAO_CURA", 0.30);
			return new Efeito("Hemorragia", TipoEfeito.DOT, 500, mods, 0, TICK_INTERVAL);
		});

		// =====================================================================
		// EFEITOS EXTRAS (CC / Utility)
		// =====================================================================

		// Stun (Grau 2): Incapacita o alvo, próximo turno pulado
		presets.put("Stun (Atordoado)", (duracao, danoDaSource) ->
			new Efeito("Stun", TipoEfeito.DEBUFF, 100, null, 0, 0)
		);

		// Lento (Grau 1): Aumenta custo em TU das habilidades (+30%)
		presets.put("Lento", (duracao, danoDaSource) ->
			new Efeito("Lento", TipoEfeito.DEBUFF, duracao, null, 0, 0)
		);

		// Charm (Grau 1): Acúmulos baseados em Carisma. 100 acúmulos = transe de 100 TU
		presets.put("Charm", (duracao, danoDaSource) ->
			new Efeito("Charm", TipoEfeito.DEBUFF, 9999, null, 0, 0)
		);

		// =====================================================================
		// BUFFS DE STATUS
		// =====================================================================

		presets.put("Força Ampliada (+20% Dano)",
				(duracao, valor) -> createMod(duracao, TipoEfeito.BUFF, "DANO_BONUS_PERCENTUAL", 0.20));
		presets.put("Força Ampliada V2 (+40% Dano)",
				(duracao, valor) -> createMod(duracao, TipoEfeito.BUFF, "DANO_BONUS_PERCENTUAL", 0.40));
		presets.put("Escudo Divino (+Armadura)",
				(duracao, valor) -> createMod(duracao, TipoEfeito.BUFF, "ARMADURA_TOTAL", (double) valor));
		presets.put("Velocidade (+Movimento)",
				(duracao, valor) -> createMod(duracao, TipoEfeito.BUFF, "MOVIMENTO", 2.0));

		// --- EFEITOS ESPECIAIS DE CLASSE ---
		presets.put("Stealth", (duracao, valor) -> new Efeito("Stealth", TipoEfeito.BUFF, duracao, null, 0, 0));
		presets.put("Bênção da Vigília",
				(duracao, valor) -> createMod(duracao, TipoEfeito.BUFF, "ARMADURA_TOTAL", 20.0));
		presets.put("Tiro Especial",
				(duracao, valor) -> new Efeito("Tiro Especial", TipoEfeito.BUFF, duracao, null, 0, 0));
		presets.put("Combo!", (duracao, valor) -> new Efeito("Combo!", TipoEfeito.BUFF, duracao, null, 0, 0));
		presets.put("Restrição Celestial",
				(duracao, valor) -> new Efeito("Restrição Celestial", TipoEfeito.BUFF, duracao, null, 0, 0));
		presets.put("Guardião", (duracao, valor) -> new Efeito("Guardião", TipoEfeito.BUFF, duracao, null, 0, 0));
		presets.put("Escudo do Campeão",
				(duracao, valor) -> createMod(duracao, TipoEfeito.BUFF, "ARMADURA_TOTAL", 20.0));

		// =====================================================================
		// DEBUFFS DE REDUÇÃO DE CURA (via modificador REDUCAO_CURA)
		// =====================================================================

		presets.put("Ruptura (Cura/Defesa Reduzida)", (duracao, valor) -> {
			Map<String, Double> mods = new HashMap<>();
			mods.put("REDUCAO_CURA", 0.50);
			return new Efeito("Ruptura", TipoEfeito.DEBUFF, duracao, mods, 0, 0);
		});
		presets.put("Dilaceramento", (duracao, valor) -> {
			Map<String, Double> mods = new HashMap<>();
			mods.put("REDUCAO_CURA", 0.25);
			return new Efeito("Dilaceramento", TipoEfeito.DEBUFF, duracao, mods, 0, 0);
		});
		presets.put("Corta Cura", (duracao, valor) -> {
			Map<String, Double> mods = new HashMap<>();
			mods.put("REDUCAO_CURA", 0.25);
			return new Efeito("Corta Cura", TipoEfeito.DEBUFF, duracao, mods, 0, 0);
		});
		presets.put("Corta Cura+", (duracao, valor) -> {
			Map<String, Double> mods = new HashMap<>();
			mods.put("REDUCAO_CURA", 0.40);
			return new Efeito("Corta Cura+", TipoEfeito.DEBUFF, duracao, mods, 0, 0);
		});

		presets.put("Marca do Deserto",
				(duracao, valor) -> new Efeito("Marca do Deserto", TipoEfeito.DEBUFF, duracao, null, 0, 0));
		presets.put("Armadura Quebrada (-30%)", (duracao, valor) -> {
			Map<String, Double> mods = new HashMap<>();
			mods.put("REDUCAO_DANO_MODIFICADOR", -0.30);
			return new Efeito("Armadura Quebrada", TipoEfeito.DEBUFF, duracao, mods, 0, 0);
		});
	}

	// Helper para criar efeitos com um modificador simples
	private static Efeito createMod(int duracao, TipoEfeito tipo, String chave, Double val) {
		Map<String, Double> mods = new HashMap<>();
		mods.put(chave, val);
		return new Efeito("Preset", tipo, duracao, mods, 0, 0);
	}

	public static Set<String> getNomesPresets() {
		return presets.keySet();
	}

	public static Efeito criarEfeito(String nomePreset, int duracao, int valorAuxiliar) {
		if (presets.containsKey(nomePreset)) {
			Efeito e = presets.get(nomePreset).create(duracao, valorAuxiliar);
			if (e.getNome().equals("Preset")) {
				return new Efeito(nomePreset, e.getTipo(), e.getDuracaoTUInicial(), e.getModificadores(),
						e.getDanoPorTick(), e.getIntervaloTickTU());
			}
			return e;
		}
		return new Efeito(nomePreset, TipoEfeito.DEBUFF, duracao, null, 0, 0);
	}

	private interface EffectCreator {
		Efeito create(int duracao, int valor);
	}
}

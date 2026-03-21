package br.com.dantesrpg.model.util;

import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.enums.TipoEfeito;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashMap;

public class EffectFactory {

	private static final Map<String, EffectCreator> presets = new LinkedHashMap<>();

	static {
		// --- EFEITOS DE DANO (DOT) ---
		presets.put("Sangramento",
				(duracao, valor) -> new Efeito("Sangramento", TipoEfeito.DOT, duracao, null, valor, 100));
		presets.put("Veneno", (duracao, valor) -> new Efeito("Veneno", TipoEfeito.DOT, duracao, null, valor, 100));
		presets.put("Queimadura",
				(duracao, valor) -> new Efeito("Queimadura", TipoEfeito.DOT, duracao, null, valor, 100));
		presets.put("Toxina", (duracao, valor) -> new Efeito("Toxina", TipoEfeito.DOT, duracao, null, valor, 50));
		presets.put("Choque", (duracao, valor) -> new Efeito("Choque", TipoEfeito.DOT, duracao, null, valor, 10));

		// --- BUFFS DE STATUS ---
		presets.put("Força Ampliada (+20% Dano)",
				(duracao, valor) -> createBuff(duracao, "DANO_BONUS_PERCENTUAL", 0.20));
		presets.put("Força Ampliada V2 (+40% Dano)",
				(duracao, valor) -> createBuff(duracao, "DANO_BONUS_PERCENTUAL", 0.40));
		presets.put("Escudo Divino (+Armadura)",
				(duracao, valor) -> createBuff(duracao, "ARMADURA_TOTAL", (double) valor));
		presets.put("Velocidade (+Movimento)", (duracao, valor) -> createBuff(duracao, "MOVIMENTO", 2.0));

		// --- EFEITOS ESPECIAIS DE CLASSE (Para aplicar manualmente se bugar) ---
		presets.put("Stealth", (duracao, valor) -> new Efeito("Stealth", TipoEfeito.BUFF, duracao, null, 0, 0));

		presets.put("Bênção da Vigília", (duracao, valor) -> createBuff(duracao, "ARMADURA_TOTAL", 20.0));
		presets.put("Tiro Especial",
				(duracao, valor) -> new Efeito("Tiro Especial", TipoEfeito.BUFF, duracao, null, 0, 0));
		presets.put("Combo!", (duracao, valor) -> new Efeito("Combo!", TipoEfeito.BUFF, duracao, null, 0, 0));
		presets.put("Restrição Celestial",
				(duracao, valor) -> new Efeito("Restrição Celestial", TipoEfeito.BUFF, duracao, null, 0, 0));
		presets.put("Guardião", (duracao, valor) -> new Efeito("Guardião", TipoEfeito.BUFF, duracao, null, 0, 0));

		// --- DEBUFFS ---
		presets.put("Stun (Atordoado)", (duracao, valor) -> new Efeito("Stun", TipoEfeito.DEBUFF, duracao, null, 0, 0));
		presets.put("Sono", (duracao, valor) -> new Efeito("Sono", TipoEfeito.DEBUFF, duracao, null, 0, 0));
		presets.put("Lento", (duracao, valor) -> new Efeito("Lento", TipoEfeito.DEBUFF, duracao, null, 0, 0));
		presets.put("Ruptura (Cura/Defesa Reduzida)",
				(duracao, valor) -> new Efeito("Ruptura", TipoEfeito.DEBUFF, duracao, null, 0, 0));
		presets.put("Dilaceramento",
				(duracao, valor) -> new Efeito("Dilaceramento", TipoEfeito.DEBUFF, duracao, null, 0, 0));
		presets.put("Marca do Deserto",
				(duracao, valor) -> new Efeito("Marca do Deserto", TipoEfeito.DEBUFF, duracao, null, 0, 0));
		presets.put("Armadura Quebrada (-30%)",
				(duracao, valor) -> createBuff(duracao, "REDUCAO_DANO_MODIFICADOR", -0.30));
	}

	// Helper para criar buffs simples
	private static Efeito createBuff(int duracao, String chave, Double val) {
		Map<String, Double> mods = new HashMap<>();
		mods.put(chave, val);
		return new Efeito("Buff Manual", TipoEfeito.BUFF, duracao, mods, 0, 0);
	}

	public static Set<String> getNomesPresets() {
		return presets.keySet();
	}

	public static Efeito criarEfeito(String nomePreset, int duracao, int valorAuxiliar) {
		if (presets.containsKey(nomePreset)) {
			Efeito e = presets.get(nomePreset).create(duracao, valorAuxiliar);
			if (e.getNome().equals("Buff Manual")) {
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

package br.com.dantesrpg.model.racas;

import br.com.dantesrpg.controller.CombatController;
import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.Atributo;
import br.com.dantesrpg.model.enums.TipoEfeito;
import br.com.dantesrpg.model.habilidades.raciais.Transformar;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Lobisomem extends Raça {

	private int stacksAoTransformar = 0;

	public Lobisomem() {
		this.maxStacks = 5;
		this.currentStacks = 0;
	}

	@Override
	public String getNome() {
		return "Lobisomem";
	}

	@Override
	public String getDescricaoPassiva() {
		if (isV2) {
			return "Lua Sangrenta: Acúmulos infinitos (barra até 25). +2 ao sofrer dano, +1 ao causar dano. "
					+ "Transformar: +20% HP e +5% dano por acúmulo. Duração: 200TU + 50TU por acúmulo. Consome todos.";
		}
		return "Instinto Selvagem: +1 acúmulo ao sofrer dano (max 5). Com 5, pode Transformar: "
				+ "+100% HP máxima, +10% dano físico por acúmulo. Transformado: -1 acúmulo/turno, +2 ao sofrer dano.";
	}

	@Override
	public String getNomeV2() {
		return "Lua Sangrenta";
	}

	// ==================== V1 ====================
	// - Ao receber dano: +1 acúmulo (max 5)
	// - Ao transformar com 5 acúmulos: +100% HP máxima, +10% dano físico por acúmulo (50% total)
	// - Transformado: -1 acúmulo/turno, +2 ao sofrer dano ao invés de 1
	// - Sem limite de tempo, sai quando acúmulos = 0

	// ==================== V2 ====================
	// - Acúmulos infinitos, barra mostra até 25
	// - Sempre pode transformar
	// - Ao sofrer dano: +2 acúmulos
	// - Ao causar dano: +1 acúmulo por alvo atingido
	// - Ao transformar: +20% HP máxima e +5% dano por acúmulo, duração 200TU + 50TU/acúmulo
	// - Consome todos os acúmulos ao transformar

	@Override
	public void onDamageTaken(Personagem personagem, Personagem atacante, double danoRecebido, EstadoCombate estado,
			CombatManager manager) {
		if (danoRecebido <= 0)
			return;

		if (isV2) {
			// V2: sempre +2 acúmulos ao sofrer dano
			this.currentStacks += 2;
			System.out.println(">>> LUA SANGRENTA: +2 acúmulos (dano recebido). Total: " + this.currentStacks);
		} else {
			// V1: +1 acúmulo ao sofrer dano (+2 se transformado)
			int ganho = this.isTransformed ? 2 : 1;
			this.currentStacks = Math.min(this.currentStacks + ganho, this.maxStacks);
			System.out.println(">>> LOBISOMEM: +" + ganho + " acúmulo (dano recebido). Total: "
					+ this.currentStacks + "/" + this.maxStacks);
		}
	}

	@Override
	public void onDamageDealt(Personagem personagem, Personagem alvo, double danoCausado, EstadoCombate estado,
			CombatController controller) {
		if (!isV2 || danoCausado <= 0)
			return;

		// V2: +1 acúmulo ao causar dano (por alvo atingido, como half-angels)
		this.currentStacks++;
		System.out.println(">>> LUA SANGRENTA: +1 acúmulo (dano causado). Total: " + this.currentStacks);
	}

	@Override
	public void onTurnStart(Personagem personagem, EstadoCombate estado) {
		if (!isV2 && this.isTransformed) {
			// V1: -1 acúmulo por turno
			this.currentStacks--;
			System.out.println(">>> LOBISOMEM (Transformado): -1 acúmulo (manutenção). Restam: " + this.currentStacks);
			if (this.currentStacks <= 0) {
				this.currentStacks = 0;
				sairTransformacao(personagem);
			}
		}
	}

	@Override
	public void onTimeAdvanced(Personagem personagem, EstadoCombate estado, CombatController controller) {
		if (!isV2 || !this.isTransformed)
			return;

		// V2: duração baseada em TU, decrementa a cada TU
		// Usamos o efeito para controlar a duração - quando o efeito expira, sai da transformação
		Efeito forma = personagem.getEfeitosAtivos().get("Forma Lupina");
		if (forma != null && forma.getDuracaoTURestante() <= 0) {
			sairTransformacao(personagem);
		}
	}

	public boolean podeTransformar() {
		if (this.isTransformed)
			return false;
		if (isV2)
			return true;
		return this.currentStacks >= 5;
	}

	public void ativarTransformacao(Personagem p) {
		if (!podeTransformar())
			return;

		this.isTransformed = true;
		this.stacksAoTransformar = this.currentStacks;

		if (isV2) {
			ativarTransformacaoV2(p);
		} else {
			ativarTransformacaoV1(p);
		}

		// Notifica o Fantasma Nobre sobre a transformação
		if (p.getFantasmaNobre() != null) {
			p.getFantasmaNobre().onRaceTransformation(p, true);
		}
	}

	private void ativarTransformacaoV1(Personagem p) {
		System.out.println(">>> " + p.getNome() + " se TRANSFORMOU em Lobisomem!");

		// +100% de vida máxima (incluído como modificador do efeito para sobreviver ao recálculo)
		double bonusVida = p.getVidaMaxima(); // 100%

		// +10% dano físico por acúmulo
		double bonusDano = this.currentStacks * 0.10;

		Map<String, Double> mods = new HashMap<>();
		mods.put("DANO_BONUS_PERCENTUAL", bonusDano);
		mods.put("HP_MAXIMO", bonusVida);

		// Efeito sem duração definida (V1 não tem limite de tempo, sai por stacks)
		Efeito formaLupina = new Efeito("Forma Lupina", TipoEfeito.BUFF, 99999, mods, 0, 0);
		p.adicionarEfeito(formaLupina);

		// Aumenta vida atual pelo bônus (adicionarEfeito já recalculou vidaMaxima)
		p.setVidaAtual(p.getVidaAtual() + bonusVida, null, null);

		System.out.println(">>> LOBISOMEM: +" + (int) bonusVida + " HP máxima (100%). HP: "
				+ (int) p.getVidaAtual() + "/" + (int) p.getVidaMaxima());
		System.out.println(">>> LOBISOMEM: +" + (int) (bonusDano * 100) + "% dano físico ("
				+ this.currentStacks + " acúmulos x 10%).");
	}

	private void ativarTransformacaoV2(Personagem p) {
		System.out.println(">>> " + p.getNome() + " ativou a LUA SANGRENTA!");

		int stacks = this.currentStacks;

		// +20% de vida máxima por acúmulo (incluído como modificador do efeito para sobreviver ao recálculo)
		double bonusVidaPct = stacks * 0.20;
		double bonusVida = p.getVidaMaxima() * bonusVidaPct;

		// +5% dano por acúmulo
		double bonusDano = stacks * 0.05;

		Map<String, Double> mods = new HashMap<>();
		mods.put("DANO_BONUS_PERCENTUAL", bonusDano);
		mods.put("HP_MAXIMO", bonusVida);

		// Duração: 200TU base + 50TU por acúmulo
		int duracao = 200 + (stacks * 50);

		Efeito formaLupina = new Efeito("Forma Lupina", TipoEfeito.BUFF, duracao, mods, 0, 0);
		p.adicionarEfeito(formaLupina);

		// Aumenta vida atual pelo bônus (adicionarEfeito já recalculou vidaMaxima)
		p.setVidaAtual(p.getVidaAtual() + bonusVida, null, null);

		System.out.println(">>> LUA SANGRENTA: +" + (int) (bonusVidaPct * 100) + "% HP máxima (+"
				+ (int) bonusVida + " HP). " + stacks + " acúmulos x 20%.");
		System.out.println(">>> LUA SANGRENTA: +" + (int) (bonusDano * 100) + "% dano ("
				+ stacks + " acúmulos x 5%). Duração: " + duracao + "TU.");

		// Consome todos os acúmulos
		this.currentStacks = 0;
		System.out.println(">>> LUA SANGRENTA: Todos os acúmulos consumidos.");
	}

	public void sairTransformacao(Personagem p) {
		this.isTransformed = false;
		System.out.println(">>> " + p.getNome() + " voltou ao normal (Forma Lupina expirou).");

		// removerEfeito chama recalcularAtributosEstatisticas, que reseta vidaMaxima
		// sem o HP_MAXIMO do efeito e clampeia vidaAtual automaticamente
		p.removerEfeito("Forma Lupina");

		this.stacksAoTransformar = 0;

		// Notifica o Fantasma Nobre sobre o fim da transformação
		if (p.getFantasmaNobre() != null) {
			p.getFantasmaNobre().onRaceTransformation(p, false);
		}
	}

	@Override
	public int getMaxStacks() {
		if (isV2) {
			return 25; // Barra mostra até 25
		}
		return this.maxStacks;
	}

	@Override
	public List<Habilidade> getRacialAbilities(Personagem personagem) {
		Transformar transformar = new Transformar();
		if (isV2) {
			transformar.setDescricao("Ativa a Lua Sangrenta. +20% HP e +5% dano por acúmulo. "
					+ "Duração: 200TU + 50TU por acúmulo. Consome todos os acúmulos.");
		}
		return java.util.Arrays.asList(transformar);
	}
}

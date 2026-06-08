package br.com.dantesrpg.model.racas;

import br.com.dantesrpg.controller.CombatController;
import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.Atributo;
import br.com.dantesrpg.model.enums.TipoEfeito;
import br.com.dantesrpg.model.habilidades.raciais.DevilTrigger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HalfDemon extends Raça {

	public HalfDemon() {
		this.maxStacks = 10;
		this.currentStacks = 0;
	}

	@Override
	public String getNome() {
		return "Half-Demon";
	}

	@Override
	public String getDescricaoPassiva() {
		if (isV2) {
			return "Ουράνιο Χάος: +25% Crítico. DT transforma com 5. +100% Dano Crítico transformado. Matar = 10% HP escudo + 5 stacks DT.";
		}
		return "Devil Trigger: +25% Crítico. Acertos geram DT. Transforma com 5. Sobrecarga acima de 10 gera Escudo de Sangue.";
	}

	@Override
	public String getNomeV2() {
		return "Ουράνιο Χάος";
	}

	// Passiva Inata: +25% de Taxa Crítica Permanente
	@Override
	public double getBonusDanoPercentual(Personagem personagem) {
		return 0.0;
	}

	@Override
	public Map<Atributo, Integer> getAttributeModifiers(Personagem personagem) {
		return null;
	}

	@Override
	public void onTurnStart(Personagem personagem, EstadoCombate estado) {

		// Manutenção da Transformação
		if (this.isTransformed) {
			this.currentStacks--;
			System.out.println(">>> Devil Trigger: -1 Stack (Manutenção). Restam: " + this.currentStacks);
			if (this.currentStacks <= 0) {
				sairFormaDemoniaca(personagem);
			}
		}
	}

	@Override
	public void onDamageDealt(Personagem personagem, Personagem alvo, double danoCausado, EstadoCombate estado,
			CombatController controller) {
		// Acúmulos são gerados apenas por críticos (onCriticalHit)
	}

	@Override
	public void onCriticalHit(Personagem personagem, Personagem alvo, EstadoCombate estado) {
		int qtd = this.isTransformed ? 2 : 1;
		gerarAcumulo(personagem, qtd);

	}

	private void gerarAcumulo(Personagem p, int quantidade) {
		for (int i = 0; i < quantidade; i++) {
			if (this.currentStacks < this.maxStacks) {
				this.currentStacks++;
			} else {
				if (this.isTransformed) {
					aplicarSobrecarga(p);
				}
			}
		}
		System.out.println(">>> Devil Trigger: " + this.currentStacks + "/" + this.maxStacks + " Stacks.");
	}

	private void aplicarSobrecarga(Personagem p) {
		double valorEscudo = p.getVidaMaxima() * 0.05; // 5% do HP Max

		p.adicionarEscudoSangue(valorEscudo);

		System.out.println(">>> SOBRECARGA DEMONÍACA! Escudo de Sangue +" + (int) valorEscudo);

		// Efeito visual apenas para indicar na HUD
		if (!p.getEfeitosAtivos().containsKey("Sobrecarga Demoníaca")) {
			p.adicionarEfeito(new Efeito("Sobrecarga Demoníaca", TipoEfeito.BUFF, 100, null, 0, 0));
		} else {
			p.getEfeitosAtivos().get("Sobrecarga Demoníaca").setDuracaoTURestante(100);
		}
	}

	public void ativarDevilTrigger(Personagem p) {
		this.isTransformed = true;
		System.out.println(">>> " + p.getNome() + " PUXOU O DEVIL TRIGGER!");

		// Bônus: Dano Crit + Atributos. V2 (Ουράνιο Χάος): +100% Dano Crit total
		Map<String, Double> mods = new HashMap<>();
		mods.put("DANO_CRITICO", isV2 ? 1.00 : 0.75);

		// Calcula 25% dos atributos base atuais
		int forca = p.getAtributosFinais().getOrDefault(Atributo.FORCA, 0);
		int insp = p.getAtributosFinais().getOrDefault(Atributo.INSPIRACAO, 0);
		int sag = p.getAtributosFinais().getOrDefault(Atributo.SAGACIDADE, 0);

		mods.put("FORCA", forca * 0.25);
		mods.put("INSPIRACAO", insp * 0.25);
		mods.put("SAGACIDADE", sag * 0.25);

		Efeito dtForm = new Efeito("Forma Demoníaca", TipoEfeito.BUFF, 99999, mods, 0, 0);
		p.adicionarEfeito(dtForm);
		p.recalcularAtributosEstatisticas();
	}

	public void sairFormaDemoniaca(Personagem p) {
		this.isTransformed = false;
		System.out.println(">>> Devil Trigger expirou.");
		p.removerEfeito("Forma Demoníaca");
		p.recalcularAtributosEstatisticas();
	}

	@Override
	public void onKill(Personagem personagem, Personagem alvoMorto, EstadoCombate estado, CombatManager manager) {
		if (!isV2 || !this.isTransformed)
			return;

		// V2 (Ουράνιο Χάος): +10% max HP como escudo de sangue + 5 stacks DT
		double escudo = personagem.getVidaMaxima() * 0.10;
		personagem.adicionarEscudoSangue(escudo);
		System.out.println(">>> ΟΥΡΆΝΙΟ ΧΆΟΣ: Kill! Escudo de Sangue +" + (int) escudo);

		// Adiciona 5 stacks (respeitando maxStacks)
		for (int i = 0; i < 5; i++) {
			if (this.currentStacks < this.maxStacks) {
				this.currentStacks++;
			}
		}
		System.out.println(">>> ΟΥΡΆΝΙΟ ΧΆΟΣ: +5 DT Stacks. Total: " + this.currentStacks + "/" + this.maxStacks);
	}

	@Override
	public List<br.com.dantesrpg.model.Habilidade> getRacialAbilities(Personagem personagem) {
		DevilTrigger dt = new DevilTrigger();
		if (isV2) {
			dt.setDescricao("Consome 5 acúmulos para liberar Ουράνιο Χάος: +100% Dano Crítico, +25% Atributos. Matar = +10% HP Escudo + 5 stacks.");
		}
		return java.util.Arrays.asList(dt);
	}
}
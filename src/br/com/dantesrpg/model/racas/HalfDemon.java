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
		return "Devil Trigger: +25% Crítico. Acertos geram DT. Transforma com 5. Sobrecarga acima de 10 gera Escudo de Sangue.";
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
		Personagem p = personagem;
		if (danoCausado > 0) {
			gerarAcumulo(personagem, 1);
			if (this.currentStacks > 9 && this.isTransformed) {
				aplicarSobrecarga(p);
			}
		}
	}

	@Override
	public void onCriticalHit(Personagem personagem, Personagem alvo, EstadoCombate estado) {
		int qtd = this.isTransformed ? 2 : 1;
		gerarAcumulo(personagem, qtd);

	}

	private void gerarAcumulo(Personagem p, int quantidade) {
		for (int i = 0; i < quantidade; i++) {
			this.currentStacks++;
		}
		System.out.println(">>> Devil Trigger: " + this.currentStacks + " Stacks.");
	}

	private void aplicarSobrecarga(Personagem p) {
		double valorEscudo = p.getVidaMaxima() * 0.05; // 5% do HP Max

		// Ativa a flag de Escudo de Sangue
		p.setTemEscudoDeSangue(true);

		// Adiciona ao escudo atual
		p.setEscudoAtual(p.getEscudoAtual() + valorEscudo);

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

		// Bônus: +30% Crit (Total 55%), +75% Dano Crit, +25% ST/IS/SA
		Map<String, Double> mods = new HashMap<>();
		mods.put("TAXA_CRITICA", 0.25);
		mods.put("DANO_CRITICO", 0.75);

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
	public List<br.com.dantesrpg.model.Habilidade> getRacialAbilities(Personagem personagem) {
		return java.util.Arrays.asList(new DevilTrigger());
	}
}
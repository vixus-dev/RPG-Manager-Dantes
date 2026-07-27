package br.com.dantesrpg.model.racas;

import br.com.dantesrpg.controller.CombatController;
import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.Raça;
import br.com.dantesrpg.model.enums.Atributo;
import br.com.dantesrpg.model.enums.TipoEfeito;
import br.com.dantesrpg.model.habilidades.raciais.DevilTrigger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HalfDemon extends Raça {

	private static final double BONUS_DADO_TRANSFORMACAO = 0.25;
	private static final double ESCUDO_SANGUE_POR_EXCESSO = 0.05;

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
			return "Ουράνιο Χάος: +25% Taxa Crítica. Fora da forma, críticos geram 2 acúmulos; transformado, 1. Com 10 acúmulos, críticos excedentes geram Escudo de Sangue. Transformado: +125% Dano Crítico, +30% Taxa Crítica e +25% nos dados de Força, Inspiração e Sagacidade. Abates geram +1 acúmulo.";
		}
		return "Devil Trigger: +25% Taxa Crítica. Críticos geram 1 acúmulo. Transforma com 5. Transformado: +75% Dano Crítico, +30% Taxa Crítica e +25% nos dados de Força, Inspiração e Sagacidade. Com 10 acúmulos não acontece nada.";
	}

	@Override
	public String getNomeV2() {
		return "Ουράνιο Χάος";
	}

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
		if (!isTransformed) {
			return;
		}

		currentStacks--;
		System.out.println(">>> Devil Trigger: -1 acúmulo (manutenção). Restam: " + currentStacks);
		if (currentStacks <= 0) {
			currentStacks = 0;
			sairFormaDemoniaca(personagem);
		}
	}

	@Override
	public void onDamageDealt(Personagem personagem, Personagem alvo, double danoCausado, EstadoCombate estado,
			CombatController controller) {
		// Os acúmulos são gerados exclusivamente pelo hook de acerto crítico.
	}

	@Override
	public void onCriticalHit(Personagem personagem, Personagem alvo, EstadoCombate estado) {
		// V1: +1 crítico em qualquer estado. V2: +2 fora da forma e +1 transformado.
		int quantidade = isV2 && !isTransformed ? 2 : 1;
		adicionarAcumulos(personagem, quantidade);
	}

	private void adicionarAcumulos(Personagem personagem, int quantidade) {
		for (int i = 0; i < quantidade; i++) {
			if (currentStacks < maxStacks) {
				currentStacks++;
			} else if (isV2) {
				aplicarSobrecarga(personagem);
			}
		}
		System.out.println(">>> Half-Demon: " + currentStacks + "/" + maxStacks + " acúmulos.");
	}

	private void aplicarSobrecarga(Personagem personagem) {
		double valorEscudo = personagem.getVidaMaxima() * ESCUDO_SANGUE_POR_EXCESSO;
		personagem.adicionarEscudoSangue(valorEscudo);

		System.out.println(">>> SOBRECARGA DEMONÍACA! Escudo de Sangue +" + (int) valorEscudo);
		String nomeEfeito = "Sobrecarga Demoníaca";
		Efeito efeito = personagem.getEfeitosAtivos().get(nomeEfeito);
		if (efeito == null) {
			personagem.adicionarEfeito(new Efeito(nomeEfeito, TipoEfeito.BUFF, 100, null, 0, 0));
		} else {
			efeito.setDuracaoTURestante(100);
		}
	}

	public void ativarDevilTrigger(Personagem personagem) {
		isTransformed = true;
		System.out.println(">>> " + personagem.getNome() + " ativou o DEVIL TRIGGER!");

		Map<String, Double> modificadores = new HashMap<>();
		modificadores.put("DANO_CRITICO", isV2 ? 1.25 : 0.75);
		modificadores.put("TAXA_CRITICA", 0.30);

		// Mantém o cálculo atual: o bônus é baseado nos atributos finais no momento da ativação.
		int forca = personagem.getAtributosFinais().getOrDefault(Atributo.FORCA, 0);
		int inspiracao = personagem.getAtributosFinais().getOrDefault(Atributo.INSPIRACAO, 0);
		int sagacidade = personagem.getAtributosFinais().getOrDefault(Atributo.SAGACIDADE, 0);
		modificadores.put(Atributo.FORCA.name(), forca * BONUS_DADO_TRANSFORMACAO);
		modificadores.put(Atributo.INSPIRACAO.name(), inspiracao * BONUS_DADO_TRANSFORMACAO);
		modificadores.put(Atributo.SAGACIDADE.name(), sagacidade * BONUS_DADO_TRANSFORMACAO);

		Efeito forma = new Efeito("Forma Demoníaca", TipoEfeito.BUFF, 99999, modificadores, 0, 0);
		personagem.adicionarEfeito(forma);
		personagem.recalcularAtributosEstatisticas();
	}

	public void sairFormaDemoniaca(Personagem personagem) {
		isTransformed = false;
		System.out.println(">>> Devil Trigger expirou.");
		personagem.removerEfeito("Forma Demoníaca");
		personagem.recalcularAtributosEstatisticas();
	}

	@Override
	public void onKill(Personagem personagem, Personagem alvoMorto, EstadoCombate estado, CombatManager manager) {
		if (!isV2) {
			return;
		}

		// V2: cada abate concede 1 acúmulo, inclusive fora da transformação.
		adicionarAcumulos(personagem, 1);
		System.out.println(">>> Ουράνιο Χάος: abate = +1 acúmulo.");
	}

	@Override
	public List<Habilidade> getRacialAbilities(Personagem personagem) {
		DevilTrigger devilTrigger = new DevilTrigger(isV2 ? 50 : 25);
		if (isV2) {
			devilTrigger.setDescricao("Consome 50 TU e 0 Mana para liberar Ουράνιο Χάος com 5 acúmulos: +125% Dano Crítico, +30% Taxa Crítica e +25% nos dados de Força, Inspiração e Sagacidade. Críticos excedentes com 10 acúmulos geram Escudo de Sangue; abates concedem +1 acúmulo.");
		} else {
			devilTrigger.setDescricao("Consome 25 TU e 0 Mana para liberar o Devil Trigger com 5 acúmulos: +75% Dano Crítico, +30% Taxa Crítica e +25% nos dados de Força, Inspiração e Sagacidade.");
		}
		return Arrays.asList(devilTrigger);
	}
}

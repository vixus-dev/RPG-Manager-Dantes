package br.com.dantesrpg.model.habilidades;

import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoEfeito;
import br.com.dantesrpg.model.enums.TipoHabilidade;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Respirar extends Habilidade {

	private static final String EFEITO_CONTRATO = "Contrato de Vida";

	public Respirar() {
		super("Respirar....", "Cura o usuário em 30% da vida máxima e cria um contrato de vida equivalente a 10% da vida máxima.",
				TipoHabilidade.ATIVA, 1, 100, 1, TipoAlvo.SI_MESMO, 0.0, 0, Collections.emptyList());
	}

	@Override
	public boolean afetaInimigos() {
		return false;
	}

	@Override
	public boolean afetaAliados() {
		return false;
	}

	@Override
	public boolean afetaSiMesmo() {
		return true;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(conjurador.getNome() + " ativa Respirar....!");

		// Cura 30% da vida máxima
		double vidaMaxima = conjurador.getVidaMaxima();
		double cura = vidaMaxima * 0.30;
		double vidaNova = conjurador.getVidaAtual() + cura;
		conjurador.setVidaAtual(vidaNova, estado, manager.getController());
		System.out.println(">>> " + conjurador.getNome() + " recuperou " + String.format("%.0f", cura) + " HP! (Respirar....)");

		// Cria contrato de vida: reduz HP máximo em 10%
		double reducaoHp = vidaMaxima * 0.10;

		Efeito contratoExistente = conjurador.getEfeitosAtivos().get(EFEITO_CONTRATO);

		if (contratoExistente != null && contratoExistente.getModificadores() != null) {
			// Acumula a redução se já existe um contrato
			double reducaoAtual = -contratoExistente.getModificadores().getOrDefault("HP_MAXIMO", 0.0);
			double novaReducao = reducaoAtual + reducaoHp;
			contratoExistente.getModificadores().put("HP_MAXIMO", -novaReducao);
			contratoExistente.setDuracaoTURestante(99999);
			System.out.println(">>> Contrato de Vida acumulado! HP máximo reduzido em " + String.format("%.0f", novaReducao) + ".");
		} else {
			Map<String, Double> modificadores = new HashMap<>();
			modificadores.put("HP_MAXIMO", -reducaoHp);

			Efeito contrato = new Efeito(EFEITO_CONTRATO, TipoEfeito.DEBUFF, 99999, modificadores, 0, 0);
			conjurador.adicionarEfeito(contrato);
			System.out.println(">>> Contrato de Vida criado! HP máximo reduzido em " + String.format("%.0f", reducaoHp) + ".");
		}

		conjurador.recalcularAtributosEstatisticas();
	}
}

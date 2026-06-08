package br.com.dantesrpg.model.habilidades;

import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoHabilidade;
import br.com.dantesrpg.model.util.ContratoDeVida;
import br.com.dantesrpg.model.util.ContratoDeVidaUtils;

import java.util.Collections;
import java.util.List;

public class Respirar extends Habilidade {

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

		// 1) Cria (ou acumula) o contrato ANTES da cura, para que o novo teto
		//    seja respeitado quando a cura for aplicada.
		double reducaoHp = conjurador.getVidaMaxima() * 0.10;
		ContratoDeVida existente = ContratoDeVidaUtils.getContratoPorFonte(conjurador, ContratoDeVida.FONTE_RESPIRAR);
		if (existente != null) {
			existente.setValorTotal(existente.getValorTotal() + reducaoHp);
			existente.setDividaRestante(existente.getDividaRestante() + reducaoHp);
			conjurador.recalcularAtributosEstatisticas();
			System.out.println(">>> Contrato de Vida (Respirar) acumulado! Dívida total: "
					+ String.format("%.0f", existente.getDividaRestante()) + ".");
		} else {
			ContratoDeVida c = new ContratoDeVida(ContratoDeVida.FONTE_RESPIRAR, reducaoHp, -1, false);
			ContratoDeVidaUtils.adicionarContrato(conjurador, c);
			System.out.println(">>> Contrato de Vida (Respirar) criado! Dívida: "
					+ String.format("%.0f", reducaoHp) + ".");
		}

		// 2) Cura 30% da vida máxima base (cura passa pelo fluxo unificado; excedente paga dívida)
		double cura = conjurador.getVidaMaximaBase() * 0.30;
		conjurador.setVidaAtual(conjurador.getVidaAtual() + cura, estado, manager.getController());
		System.out.println(">>> " + conjurador.getNome() + " recuperou até " + String.format("%.0f", cura) + " HP (Respirar....).");
	}
}

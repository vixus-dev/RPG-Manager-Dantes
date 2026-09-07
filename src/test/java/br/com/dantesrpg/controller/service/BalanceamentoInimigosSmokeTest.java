package br.com.dantesrpg.controller.service;

import java.util.List;

import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAcao;

public final class BalanceamentoInimigosSmokeTest {
	private BalanceamentoInimigosSmokeTest() {
	}

	public static void main(String[] args) {
		exigirProximo(70.0, BestiarioSpawnService.calcularVidaBalanceada(100.0, "5"), "vida no andar 5");
		exigirProximo(70.0, BestiarioSpawnService.calcularVidaBalanceada(100.0, "1,5"), "andar decimal com vírgula");
		exigirProximo(70.0, BestiarioSpawnService.calcularVidaBalanceada(100.0, "0.5"), "andar decimal com ponto");
		exigirProximo(100.0, BestiarioSpawnService.calcularVidaBalanceada(100.0, "6"), "andar 6 excluído");
		exigirProximo(100.0, BestiarioSpawnService.calcularVidaBalanceada(100.0, "Céu 1"), "andar não numérico excluído");
		exigirProximo(100.0, BestiarioSpawnService.calcularVidaBalanceada(100.0, "Não 0"), "rótulo não numérico excluído");

		Personagem atacante = new Personagem();
		atacante.setNome("Inimigo balanceado");
		atacante.setMultiplicadorDanoCausado(0.90);
		Personagem alvo = new Personagem();
		alvo.setNome("Alvo");
		alvo.setVidaAtual(100.0);
		EstadoCombate estado = new EstadoCombate(List.of(atacante, alvo));
		CombatManager manager = new CombatManager(null);
		manager.getDamageApplicator().aplicarDanoAoAlvoResolvido(
				atacante, alvo, 100.0, true, TipoAcao.ATAQUE_BASICO, estado);
		exigirProximo(10.0, alvo.getVidaAtual(), "dano causado reduzido em 10%");

		System.out.println("BALANCEAMENTO_INIMIGOS_SMOKE_OK");
	}

	private static void exigirProximo(double esperado, double atual, String contrato) {
		if (Math.abs(esperado - atual) > 0.0001) {
			throw new AssertionError("Falha no balanceamento (" + contrato + "): esperado="
					+ esperado + ", atual=" + atual);
		}
	}
}

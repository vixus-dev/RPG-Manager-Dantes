package br.com.dantesrpg.model.habilidades;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import br.com.dantesrpg.model.map.TerrainData;
import java.util.*;

public class DoubleDown extends Habilidade {

	public DoubleDown() {
		super("DoubleDown", "Golpeia o solo criando ondas de fogo. (Requer: Modo Engaged)", TipoHabilidade.ATIVA, 2,
				110, 1, TipoAlvo.AREA_QUADRADA, 3, // Tamanho 3x3
				1, // Multiplicador de Dano (Alto)
				1, Collections.emptyList());
	}

	@Override
	public boolean afetaAliados() {
		return false; // Padrão: Habilidades de AoE causam fogo amigo
	}

	// Sobrescrevemos a versão COM COORDENADAS
	@Override
	public void executar(Personagem conjurador, int alvoX, int alvoY, List<Personagem> alvos, EstadoCombate estado,
			CombatManager manager) {

		// Validação: Modo Engaged
		if (!conjurador.getEfeitosAtivos().containsKey("Modo Engaged")) {
			System.out.println(">>> FALHA: DoubleDown requer 'Modo Engaged' ativo!");
			return;
		}

		System.out.println(
				">>> BOSS SKILL: " + conjurador.getNome() + " usa DOUBLE DOWN em (" + alvoX + "," + alvoY + ")!");

		// Causa Dano Direto (Impacto)
		Arma arma = conjurador.getArmaEquipada();
		double danoArma = (arma != null) ? arma.getDanoBase() : 10;
		if (arma != null && conjurador.getRaca() != null) {
			Personagem alvoReferencia = alvos.isEmpty() ? null : alvos.get(0);
			danoArma *= conjurador.getRaca().getMultiplicadorBonusDanoArma(conjurador, arma, alvoReferencia, estado,
					null);
		}
		double danoFinal = danoArma * getMultiplicadorDeDano();

		for (Personagem alvo : alvos) {
			manager.aplicarDanoAoAlvo(conjurador, alvo, danoFinal, false, TipoAcao.HABILIDADE, estado, 0);
		}

		// Aplica Efeito de Terreno (Fogo)
		br.com.dantesrpg.controller.CombatController main = manager.getMainController();

		if (main != null && main.getMapController() != null) {

			// Dano do fogo no chão = 30% do dano da arma
			int danoFogo = (int) Math.max(1, danoArma * 0.3);

			int raio = (getTamanhoArea() - 1) / 2; // Raio 1 para area 3x3

			for (int y = alvoY - raio; y <= alvoY + raio; y++) {
				for (int x = alvoX - raio; x <= alvoX + raio; x++) {

					// Cria uma nova instância de efeito para cada célula
					TerrainData.EfeitoInstance efeitoFogo = new TerrainData.EfeitoInstance(
							TerrainData.TipoEfeitoSolo.FOGO, 200, // Duração: 200 TU
							danoFogo, conjurador);

					// O MapController decide se vira permanente (Carvão) ou normal
					main.getMapController().aplicarEfeitoNoSolo(x, y, efeitoFogo);
				}
			}
		}
	}

	// Sobrescreve o antigo apenas para evitar bugs, mas redireciona erro se chamado sem X,Y
	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.err.println("ERRO: DoubleDown foi chamado sem coordenadas! O chão não queimará.");
		// Aplica apenas dano nos alvos como fallback
		executar(conjurador, 0, 0, alvos, estado, manager);
	}
}

package br.com.dantesrpg;

import java.util.Collections;
import java.util.List;

import br.com.dantesrpg.model.AcaoMestreInput;
import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoHabilidade;
import br.com.dantesrpg.model.map.CoordenadaMapa;

public final class MultiAoEContractSmokeTest {
	private MultiAoEContractSmokeTest() {
	}

	public static void main(String[] args) {
		Personagem ator = new Personagem();
		Personagem alvoSobreposto = new Personagem();
		Habilidade habilidade = criarHabilidadeMultiAoE(TipoAlvo.AREA_CIRCULAR);

		AcaoMestreInput.AreaSelecionada area1 = new AcaoMestreInput.AreaSelecionada(
				new CoordenadaMapa(3, 4), List.of(alvoSobreposto));
		AcaoMestreInput.AreaSelecionada area2 = new AcaoMestreInput.AreaSelecionada(
				new CoordenadaMapa(4, 4), List.of(alvoSobreposto));

		AcaoMestreInput input = new AcaoMestreInput(ator, Collections.emptyList(), habilidade);
		input.setAreasSelecionadas(List.of(area1, area2));

		exigir(habilidade.getTipoAlvo() == TipoAlvo.MULTI_AOE, "tipo principal");
		exigir(habilidade.getTipoAlvoEfetivo() == TipoAlvo.AREA_CIRCULAR, "subtipo efetivo");
		exigir(habilidade.getNumeroDeAreas() == 2, "quantidade de áreas");
		exigir(input.getEpicentros().equals(List.of(new CoordenadaMapa(3, 4), new CoordenadaMapa(4, 4))),
				"ordem dos epicentros");
		exigir(input.getAlvos().size() == 2, "duas ocorrências do alvo sobreposto");
		exigir(input.getAlvos().get(0) == alvoSobreposto && input.getAlvos().get(1) == alvoSobreposto,
				"identidade do alvo sobreposto");
		exigir(input.getNumeroAreaDoImpacto(0) == 1, "origem do primeiro impacto");
		exigir(input.getNumeroAreaDoImpacto(1) == 2, "origem do segundo impacto");

		boolean subtipoInvalidoRejeitado = false;
		try {
			criarHabilidadeMultiAoE(TipoAlvo.INDIVIDUAL).getTipoAlvoEfetivo();
		} catch (IllegalStateException esperado) {
			subtipoInvalidoRejeitado = true;
		}
		exigir(subtipoInvalidoRejeitado, "rejeição de subtipo inválido");

		System.out.println("MULTI_AOE_CONTRACT_SMOKE_OK");
	}

	private static Habilidade criarHabilidadeMultiAoE(TipoAlvo subtipo) {
		return new Habilidade("Teste Multi-AOE", "", TipoHabilidade.ATIVA,
				0, 100, 1, TipoAlvo.MULTI_AOE, 3, 1.0, 1, Collections.emptyList()) {
			@Override
			public TipoAlvo getSubtipoArea() {
				return subtipo;
			}

			@Override
			public int getNumeroDeAreas() {
				return 2;
			}

			@Override
			public void executar(Personagem conjurador, List<Personagem> alvos,
					EstadoCombate estado, CombatManager manager) {
			}
		};
	}

	private static void exigir(boolean condicao, String contrato) {
		if (!condicao) {
			throw new AssertionError("Falha no contrato MULTI_AOE: " + contrato);
		}
	}
}

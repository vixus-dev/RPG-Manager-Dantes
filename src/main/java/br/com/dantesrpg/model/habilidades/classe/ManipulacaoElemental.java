package br.com.dantesrpg.model.habilidades.classe;

import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoHabilidade;

import java.util.Collections;
import java.util.List;

public class ManipulacaoElemental extends Habilidade {
	public static final String BOLA = "Bola 3x3";
	public static final String QUADRADO = "Quadrado 3x3";
	public static final String LINHA = "Linha 4x1";
	public static final String INDIVIDUAL = "Individual";

	private String opcaoSelecionada = BOLA;

	public ManipulacaoElemental() {
		super("Manipulação elemental",
				"Escolha entre bola, quadrado, linha ou alvo individual. Causa 1,35x de dano.",
				TipoHabilidade.ATIVA, 1, 120, 3, TipoAlvo.INDIVIDUAL, 0, 1.35, 1,
				Collections.emptyList());
	}

	public void setOpcaoSelecionada(String opcao) {
		if (getOpcoesSelection().contains(opcao)) {
			opcaoSelecionada = opcao;
		}
	}

	public String getOpcaoSelecionada() {
		return opcaoSelecionada;
	}

	@Override
	public List<String> getOpcoesSelection() {
		return List.of(BOLA, QUADRADO, LINHA, INDIVIDUAL);
	}

	@Override
	public TipoAlvo getTipoAlvo() {
		return switch (opcaoSelecionada) {
		case QUADRADO -> TipoAlvo.AREA_QUADRADA;
		case LINHA -> TipoAlvo.LINHA;
		case BOLA -> TipoAlvo.AREA_CIRCULAR;
		default -> TipoAlvo.INDIVIDUAL;
		};
	}

	@Override
	public int getTamanhoArea() {
		return switch (opcaoSelecionada) {
		case BOLA, QUADRADO -> 3;
		case LINHA -> 1;
		default -> 0;
		};
	}

	@Override
	public int getAlcanceMaximo() {
		return 4;
	}

	@Override
	public int getCustoTUModificado(Personagem conjurador) {
		boolean transformado = conjurador != null && conjurador.getRaca() != null
				&& conjurador.getRaca().isTransformed();
		boolean mastermindAtivo = conjurador != null
				&& conjurador.getEfeitosAtivos().containsKey(Mastermind.NOME_EFEITO);
		return transformado && mastermindAtivo ? Math.max(0, getCustoTU() - 20) : getCustoTU();
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado,
			CombatManager manager) {
		System.out.println(">>> " + (conjurador == null ? "Personagem" : conjurador.getNome())
				+ " usa Manipulação elemental: " + opcaoSelecionada + ".");
	}
}

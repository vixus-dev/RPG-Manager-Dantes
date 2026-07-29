package br.com.dantesrpg.model.habilidades.fantasmasnobres;

import java.util.List;

import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.habilidades.classe.ManipulacaoElemental;

/** Variante sanguínea das formas de área de Manipulação Elemental. */
public class DestruirSangue extends ManipulacaoElemental {

	@Override
	public String getNome() {
		return "Destruir Sangue";
	}

	@Override
	public String getDescricao() {
		return "Escolha entre bola, quadrado, linha ou alvo individual. Causa 1,75x de dano.";
	}

	@Override
	public int getCustoMana() {
		return 1;
	}

	@Override
	public int getCustoTU() {
		return 110;
	}

	@Override
	public int getCustoTUModificado(Personagem conjurador) {
		return getCustoTU();
	}

	@Override
	public double getMultiplicadorDeDano() {
		return 1.75;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado,
			CombatManager manager) {
		System.out.println(">>> " + conjurador.getNome() + " usa Destruir Sangue: " + getOpcaoSelecionada() + ".");
	}
}

package br.com.dantesrpg.model.racas;

import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.Raça;
import br.com.dantesrpg.model.enums.TipoEfeito;
import br.com.dantesrpg.model.enums.TipoAcao;

import java.util.Collections;
import java.util.List;

public class Elfo extends Raça {

	private final String nome = "Elfo";
	private final String descricaoPassiva = "Fluxo: Ganha acúmulos ao variar tipos de ação. Com 4 acúmulos, entra no Estado Dourado.";

	private TipoAcao ultimoTipoAcao = TipoAcao.NENHUMA;

	public Elfo() {
		this.maxStacks = 4;
		this.currentStacks = 0;
	}

	@Override
	public String getNome() {
		return nome;
	}

	@Override
	public String getDescricaoPassiva() {
		return descricaoPassiva;
	}

	@Override
	public List<Habilidade> getRacialAbilities(Personagem personagem) {
		return Collections.emptyList();
	}

	@Override
	public void onActionUsed(Personagem personagem, TipoAcao tipoAcaoAnterior, TipoAcao tipoAcaoAtual,
			EstadoCombate estado) {

		// Verifica se é diferente E se não é NENHUM E se não está no Estado Dourado
		boolean acaoDiferente = (tipoAcaoAtual != this.ultimoTipoAcao);
		boolean naoEstaDourado = !personagem.getEfeitosAtivos().containsKey("Estado Dourado");

		if (acaoDiferente && naoEstaDourado) {
			this.currentStacks++;
			System.out.println(">>> ELFO (" + personagem.getNome() + "): Variou ação (" + this.ultimoTipoAcao + " -> "
					+ tipoAcaoAtual + "). Fluxo: " + this.currentStacks + "/4");

			if (this.currentStacks >= this.maxStacks) {
				ativarEstadoDourado(personagem, estado);
				this.currentStacks = 0;
			}
		} else if (!acaoDiferente) {
			System.out.println(">>> ELFO: Repetiu a ação (" + tipoAcaoAtual + "). Fluxo não aumentou.");
		}

		// Atualiza o histórico
		this.ultimoTipoAcao = tipoAcaoAtual;
	}

	private void ativarEstadoDourado(Personagem personagem, EstadoCombate estado) {
		System.out.println(">>> " + personagem.getNome() + " ativou o ESTADO DOURADO!");

		Efeito estadoDourado = new Efeito("Estado Dourado", TipoEfeito.BUFF, 300, null, 0, 0);

		personagem.adicionarEfeito(estadoDourado);
		System.out.println(">>> Efeito [Estado Dourado] aplicado por 300 TUs.");
	}

	public void onEstadoDouradoEnd(Personagem personagem, EstadoCombate estado) {
		System.out.println(">>> Estado Dourado de " + personagem.getNome() + " terminou.");
		this.currentStacks = 0;
		this.ultimoTipoAcao = TipoAcao.NENHUMA; // Reseta
	}

}
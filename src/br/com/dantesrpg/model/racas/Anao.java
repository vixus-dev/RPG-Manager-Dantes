package br.com.dantesrpg.model.racas;

import java.util.List;

import br.com.dantesrpg.model.AcaoMestreInput;
import br.com.dantesrpg.model.Arma;
import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.Raça;
import br.com.dantesrpg.model.enums.TipoAcao;
import br.com.dantesrpg.model.enums.TipoEfeito;
import br.com.dantesrpg.model.habilidades.raciais.PosturaInabalavel;

public class Anao extends Raça {

	public static final String EFEITO_POSTURA = "Postura Inabalavel";
	private static final int CUSTO_TU_EXTRA_POSTURA = 20;

	private final String nome = "Anao";
	private final String descricaoPassiva = "Obt\u00e9m a Postura Inabalavel: reduz em 50% o dano recebido antes da armadura, impede movimento, torna-se imune a movimento forçado, aumenta em 20% o dano de armas com mais de 120 TU e faz ataques custarem +20 TU.";

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
		return List.of(new PosturaInabalavel());
	}

	public boolean estaEmPostura(Personagem personagem) {
		return personagem != null && personagem.getEfeitosAtivos().containsKey(EFEITO_POSTURA);
	}

	public void togglePostura(Personagem personagem) {
		if (personagem == null) {
			return;
		}

		if (estaEmPostura(personagem)) {
			encerrarPostura(personagem);
			return;
		}

		Efeito postura = new Efeito(EFEITO_POSTURA, TipoEfeito.BUFF, 99999, null, 0, 0);
		personagem.adicionarEfeito(postura);
		System.out.println(">>> " + personagem.getNome() + " assume a " + EFEITO_POSTURA + ".");
	}

	public void encerrarPostura(Personagem personagem) {
		if (!estaEmPostura(personagem)) {
			return;
		}

		personagem.removerEfeito(EFEITO_POSTURA);
		System.out.println(">>> " + personagem.getNome() + " deixou a " + EFEITO_POSTURA + ".");
	}

	@Override
	public double getMultiplicadorBonusDanoArma(Personagem personagem, Arma arma, Personagem alvo,
			EstadoCombate estado, AcaoMestreInput input) {
		if (!estaEmPostura(personagem) || arma == null || arma.getCustoTU() <= 120) {
			return 1.0;
		}
		return 1.20;
	}

	@Override
	public double getMultiplicadorDanoRecebidoPreArmadura(Personagem personagem, Personagem atacante,
			EstadoCombate estado) {
		if (!estaEmPostura(personagem)) {
			return 1.0;
		}
		return 0.50;
	}

	@Override
	public int getCustoTUExtra(Personagem personagem, Habilidade habilidade, TipoAcao tipoAcaoAtual) {
		if (!estaEmPostura(personagem)) {
			return 0;
		}

		if (tipoAcaoAtual == TipoAcao.ATAQUE_BASICO) {
			return CUSTO_TU_EXTRA_POSTURA;
		}

		if (tipoAcaoAtual == TipoAcao.HABILIDADE && habilidade != null && habilidade.getMultiplicadorDeDano() > 0) {
			return CUSTO_TU_EXTRA_POSTURA;
		}

		return 0;
	}

	@Override
	public boolean podeSeMover(Personagem personagem) {
		return !estaEmPostura(personagem);
	}

	@Override
	public boolean isImuneMovimentoForcado(Personagem personagem) {
		return estaEmPostura(personagem);
	}
}

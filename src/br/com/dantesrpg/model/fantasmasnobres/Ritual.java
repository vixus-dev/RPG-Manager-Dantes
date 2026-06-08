package br.com.dantesrpg.model.fantasmasnobres;

import java.util.List;

import br.com.dantesrpg.model.AcaoMestreInput;
import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.FantasmaNobre;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;

public class Ritual extends FantasmaNobre {

	@Override
	public String getNome() {
		return "Ritual";
	}

	@Override
	public String getDescricao() {
		return "Forca o HP do usuario a cair para 1% da vida maxima.";
	}

	@Override
	public int getCustoMana() {
		return 0;
	}

	@Override
	public int getCustoTU() {
		return 0;
	}

	@Override
	public int getCooldownTU() {
		return 0;
	}

	@Override
	public TipoAlvo getTipoAlvo() {
		return TipoAlvo.SI_MESMO;
	}

	@Override
	public int getTamanhoArea() {
		return 0;
	}

	@Override
	public int getNumeroDeAlvos() {
		return 0;
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, AcaoMestreInput input,
			CombatManager manager) {

		double vidaMaxima = conjurador.getVidaMaxima();
		double umPorcento = vidaMaxima * 0.01;

		conjurador.setVidaAtual(umPorcento);
		System.out.println(">>> " + conjurador.getNome() + " ativou Ritual! HP reduzido para " + (int) umPorcento + ".");
	}
}

package br.com.dantesrpg.model.fantasmasnobres;

import java.util.List;

import br.com.dantesrpg.model.AcaoMestreInput;
import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.FantasmaNobre;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.habilidades.fantasmasnobres.DestruirSangue;
import br.com.dantesrpg.model.habilidades.fantasmasnobres.DobrarSangue;
import br.com.dantesrpg.model.habilidades.fantasmasnobres.PrenderSangue;

/** Fantasma Nobre passivo que concede técnicas de manipulação de sangue. */
public class BloodMoon extends FantasmaNobre {

	@Override
	public String getNome() {
		return "BloodMoon";
	}

	@Override
	public String getDescricao() {
		return "Concede Prender Sangue, Dobrar Sangue e Destruir Sangue.";
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
	public List<Habilidade> getHabilidadesConcedidas() {
		return List.of(new PrenderSangue(), new DobrarSangue(), new DestruirSangue());
	}

	@Override
	public boolean possuiAcaoAtiva() {
		return false;
	}

	@Override
	public void onDamageDealt(Personagem conjurador, Personagem alvo, double danoCausado, EstadoCombate estado,
			CombatManager manager) {
		if (danoCausado <= 0 || manager == null || manager.getLastInput() == null
				|| !(manager.getLastInput().getHabilidade() instanceof DobrarSangue)) {
			return;
		}

		double escudo = conjurador.getVidaMaxima() * 0.05;
		conjurador.adicionarEscudoSangue(escudo);
		System.out.println(">>> BloodMoon: " + conjurador.getNome() + " recebeu "
				+ String.format("%.1f", escudo) + " de escudo de sangue por atingir " + alvo.getNome() + ".");
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado,
			AcaoMestreInput input, CombatManager manager) {
		// BloodMoon é passivo; suas ações são expostas como habilidades concedidas.
	}
}

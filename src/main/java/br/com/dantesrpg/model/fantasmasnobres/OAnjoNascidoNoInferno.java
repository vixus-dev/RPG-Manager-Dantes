package br.com.dantesrpg.model.fantasmasnobres;

import java.util.List;

import br.com.dantesrpg.model.AcaoMestreInput;
import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.FantasmaNobre;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.habilidades.fantasmasnobres.DancaMetamoru;
import br.com.dantesrpg.model.habilidades.fantasmasnobres.Kamehameha;

/** FN passivo que concede as técnicas Kamehameha e Dança Metamoru. */
public class OAnjoNascidoNoInferno extends FantasmaNobre {

	private final Kamehameha kamehameha = new Kamehameha();
	private final DancaMetamoru dancaMetamoru = new DancaMetamoru();

	@Override public String getNome() { return "O Anjo Nascido no Inferno"; }
	@Override public String getDescricao() {
		return "Concede Kamehameha, um feixe carregável de Inspiração, e Dança Metamoru para fundir aliados.";
	}
	@Override public int getCustoMana() { return 0; }
	@Override public int getCustoTU() { return 0; }
	@Override public int getCooldownTU() { return 0; }
	@Override public TipoAlvo getTipoAlvo() { return TipoAlvo.SI_MESMO; }
	@Override public int getTamanhoArea() { return 0; }
	@Override public int getNumeroDeAlvos() { return 0; }
	@Override public boolean possuiAcaoAtiva() { return false; }

	@Override
	public List<Habilidade> getHabilidadesConcedidas() {
		return List.of(kamehameha, dancaMetamoru);
	}

	public void prepararMiraKamehameha(int carga) {
		kamehameha.prepararMira(carga);
		System.out.println(">>> Kamehameha carregado: selecione a linha e informe o dado de Inspiração.");
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado,
			AcaoMestreInput input, CombatManager manager) {
		// Este FN não possui ação própria; suas ações são habilidades concedidas.
	}
}

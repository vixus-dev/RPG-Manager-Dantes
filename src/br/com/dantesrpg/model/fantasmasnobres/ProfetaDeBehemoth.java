package br.com.dantesrpg.model.fantasmasnobres;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import br.com.dantesrpg.model.habilidades.classe.*;
import java.util.*;

public class ProfetaDeBehemoth extends FantasmaNobre {

	@Override
	public String getNome() {
		return "Profeta de Behemoth";
	}

	@Override
	public String getDescricao() {
		return "Espírito ancestral de Behemoth. Concede habilidades de terra que mudam ao se transformar via raça.";
	}

	// FN passivo - não é ativado diretamente
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
	public String getMotivoBloqueio(Personagem conjurador) {
		return "Profeta de Behemoth é passivo. As habilidades mudam ao se transformar via raça.";
	}

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, AcaoMestreInput input,
			CombatManager manager) {
		// FN passivo - não executa diretamente
		System.out.println(">>> Profeta de Behemoth é um Fantasma Nobre passivo.");
	}

	@Override
	public void onCombatStart(Personagem conjurador, EstadoCombate estado, CombatManager manager) {
		// Adiciona habilidades da forma normal no início do combate
		trocarParaFormaNormal(conjurador);
		System.out.println(">>> PROFETA DE BEHEMOTH: Habilidades da forma normal carregadas.");
	}

	@Override
	public void onRaceTransformation(Personagem personagem, boolean transformado) {
		if (transformado) {
			trocarParaFormaTransformada(personagem);
		} else {
			trocarParaFormaNormal(personagem);
		}
	}

	public static void trocarParaFormaNormal(Personagem p) {
		System.out.println(">>> PROFETA DE BEHEMOTH: " + p.getNome() + " - Habilidades da Forma Normal!");

		p.limparHabilidadesExtras();
		p.adicionarHabilidadeExtra(new Mergulho());
		p.adicionarHabilidadeExtra(new Pedregulho());
		p.adicionarHabilidadeExtra(new Fortificar());
	}

	public static void trocarParaFormaTransformada(Personagem p) {
		System.out.println(">>> PROFETA DE BEHEMOTH: " + p.getNome() + " - Habilidades da Forma Transformada!");

		p.limparHabilidadesExtras();
		p.adicionarHabilidadeExtra(new ParedaoDePedra());
		p.adicionarHabilidadeExtra(new Meteoro());
		p.adicionarHabilidadeExtra(new Terremoto());
	}
}

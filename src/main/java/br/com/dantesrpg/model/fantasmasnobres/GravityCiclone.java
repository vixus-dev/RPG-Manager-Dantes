package br.com.dantesrpg.model.fantasmasnobres;

import br.com.dantesrpg.model.AcaoMestreInput;
import br.com.dantesrpg.model.CombatManager;
import br.com.dantesrpg.model.Efeito;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.FantasmaNobre;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.enums.TipoEfeito;
import br.com.dantesrpg.model.map.Dominio;
import java.util.List;
import java.util.Map;

/** Domínio circular que aprisiona todos em um ciclone gravitacional. */
public class GravityCiclone extends FantasmaNobre {
	public static final String ID_DOMINIO = "gravity_ciclone_torre_devastada";
	public static final String EFEITO = "Gravity Ciclone";

	@Override public String getNome() { return "Gravity Ciclone"; }
	@Override public String getDescricao() {
		return "Cria um domínio circular 9x9 por 800 TU. Todos dentro sofrem 4 de dano a cada 25 TU; "
				+ "a borda bloqueia a saída e causa 4 de dano + Choque de 30 TU.";
	}
	@Override public int getCustoMana() { return 4; }
	@Override public int getCustoTU() { return 50; }
	@Override public int getCooldownTU() { return 0; }
	@Override public TipoAlvo getTipoAlvo() { return TipoAlvo.SI_MESMO; }
	@Override public int getTamanhoArea() { return 9; }
	@Override public int getNumeroDeAlvos() { return 0; }

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado,
			AcaoMestreInput input, CombatManager manager) {
		Efeito marcador = new Efeito(EFEITO, TipoEfeito.BUFF, 800, Map.of(), 0, 0);
		manager.getEffectProcessor().aplicarEfeito(conjurador, marcador);

		if (manager.getMainController() != null) {
			Dominio dominio = new Dominio(ID_DOMINIO, EFEITO, conjurador, conjurador.getPosX(), conjurador.getPosY(),
					getTamanhoArea(), "zona-dominio-gravity-ciclone", true);
			dominio.configurarPenalidadeBorda(4, 30);
			manager.getDomainManager().ativarDominioNoMapa(dominio, conjurador, estado);
		}
		System.out.println(">>> " + conjurador.getNome() + " abriu o domínio Gravity Ciclone.");
	}
}

package br.com.dantesrpg.model.habilidades.boss;

import br.com.dantesrpg.model.*;
import br.com.dantesrpg.model.enums.*;
import java.util.*;

public class CeifaAmpla extends Habilidade {

	public CeifaAmpla() {
		super("Ceifa Ampla", "Um golpe devastador de foice cobrindo uma grande área.", TipoHabilidade.ATIVA, 1, 115, 1,
				TipoAlvo.CONE, 0, 1.0, 1, Collections.emptyList());
	}

	@Override
	public int getAlcanceMaximo() {
		return 2;
	} // Alcance da foice

	@Override
	public int getAnguloCone() {
		return 270;
	} // Quase um círculo completo

	@Override
	public void executar(Personagem conjurador, List<Personagem> alvos, EstadoCombate estado, CombatManager manager) {
		System.out.println(">>> Zeraphon gira sua foice!");
	}
}
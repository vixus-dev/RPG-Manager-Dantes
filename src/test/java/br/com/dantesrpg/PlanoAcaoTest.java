package br.com.dantesrpg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import br.com.dantesrpg.model.AcaoMestreInput;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.combat.DamageCalculator;
import br.com.dantesrpg.model.combat.PlanoAcao;
import br.com.dantesrpg.model.enums.TipoAcao;
import br.com.dantesrpg.model.util.DamageEvent;

class PlanoAcaoTest {
	@Test
	void confirmarDuasVezesAplicaUmaUnicaVez() {
		AtomicInteger compromissos = new AtomicInteger();
		AtomicInteger municoes = new AtomicInteger();
		AtomicInteger confirmacoes = new AtomicInteger();
		PlanoAcao plano = criarPlano(compromissos, municoes, confirmacoes, () -> true);

		plano.confirmar();
		plano.confirmar();

		assertEquals(1, compromissos.get());
		assertEquals(1, municoes.get());
		assertEquals(1, confirmacoes.get());
		assertEquals(PlanoAcao.Status.APLICADO, plano.getStatus());
	}

	@Test
	void cancelarNaoExecutaNenhumaMutacao() {
		AtomicInteger compromissos = new AtomicInteger();
		AtomicInteger municoes = new AtomicInteger();
		AtomicInteger confirmacoes = new AtomicInteger();
		PlanoAcao plano = criarPlano(compromissos, municoes, confirmacoes, () -> true);

		plano.cancelar();

		assertThrows(IllegalStateException.class, plano::confirmar);
		assertEquals(0, compromissos.get());
		assertEquals(0, municoes.get());
		assertEquals(0, confirmacoes.get());
	}

	@Test
	void planoObsoletoFalhaAntesDoCommit() {
		AtomicInteger confirmacoes = new AtomicInteger();
		PlanoAcao plano = criarPlano(new AtomicInteger(), new AtomicInteger(), confirmacoes, () -> false);

		assertThrows(IllegalStateException.class, plano::confirmar);

		assertEquals(0, confirmacoes.get());
		assertEquals(PlanoAcao.Status.OBSOLETO, plano.getStatus());
	}

	private PlanoAcao criarPlano(AtomicInteger compromissos, AtomicInteger municoes,
			AtomicInteger confirmacoes, java.util.function.BooleanSupplier valido) {
		Personagem ator = new Personagem();
		ator.setNome("Ator");
		Personagem alvo = new Personagem();
		alvo.setNome("Alvo");
		AcaoMestreInput input = new AcaoMestreInput(ator, List.of(alvo), (br.com.dantesrpg.model.Habilidade) null);
		DamageCalculator.PreviaDano previa = DamageCalculator.PreviaDano.criar(
				Map.of(alvo, List.of(new DamageEvent(10, "Ataque", false, null))),
				compromissos::incrementAndGet, municoes::incrementAndGet);
		return new PlanoAcao(input, TipoAcao.ATAQUE_BASICO, null, 0, 100,
				previa, valido, confirmacoes::incrementAndGet);
	}
}

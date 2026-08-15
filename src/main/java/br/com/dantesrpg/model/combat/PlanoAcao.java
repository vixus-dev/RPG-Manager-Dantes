package br.com.dantesrpg.model.combat;

import java.time.Instant;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import br.com.dantesrpg.model.AcaoMestreInput;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.enums.TipoAcao;
import br.com.dantesrpg.model.util.DamageEvent;

/**
 * Snapshot de uma ação calculada, porém ainda não aplicada. Mantém rolagens e
 * críticos congelados até a confirmação ou cancelamento pelo mestre.
 */
public final class PlanoAcao {
	public enum Status { PENDENTE, APLICADO, CANCELADO, OBSOLETO }

	private final UUID id = UUID.randomUUID();
	private final Instant criadoEm = Instant.now();
	private final AcaoMestreInput input;
	private final TipoAcao tipoAcao;
	private final Habilidade habilidade;
	private final int custoMana;
	private final int custoTU;
	private final DamageCalculator.PreviaDano previaDano;
	private final BooleanSupplier aindaValido;
	private final Runnable aoConfirmar;
	private Status status = Status.PENDENTE;
	private Map<String, String> reacoesResolvidas = Map.of();
	private Map<String, Double> danoResolvido = Map.of();
	private List<String> filaContraAtaques = List.of();

	public PlanoAcao(AcaoMestreInput input, TipoAcao tipoAcao, Habilidade habilidade,
			int custoMana, int custoTU, DamageCalculator.PreviaDano previaDano,
			BooleanSupplier aindaValido, Runnable aoConfirmar) {
		this.input = input;
		this.tipoAcao = tipoAcao;
		this.habilidade = habilidade;
		this.custoMana = custoMana;
		this.custoTU = custoTU;
		this.previaDano = previaDano;
		this.aindaValido = aindaValido != null ? aindaValido : () -> true;
		this.aoConfirmar = aoConfirmar != null ? aoConfirmar : () -> { };
	}

	public synchronized void confirmar() {
		if (status == Status.APLICADO) {
			return;
		}
		if (status != Status.PENDENTE) {
			throw new IllegalStateException("O plano não está mais pendente: " + status);
		}
		if (!aindaValido.getAsBoolean()) {
			status = Status.OBSOLETO;
			throw new IllegalStateException("O combate mudou desde a criação da prévia. Recalcule a ação.");
		}
		previaDano.comprometerAcao();
		previaDano.confirmarMunicao();
		aoConfirmar.run();
		status = Status.APLICADO;
	}

	public synchronized void cancelar() {
		if (status == Status.PENDENTE) {
			status = Status.CANCELADO;
		}
	}

	public synchronized void registrarResultado(Map<String, String> reacoes,
			Map<String, Double> danoPorAlvo, List<String> contraAtaques) {
		this.reacoesResolvidas = reacoes == null ? Map.of()
				: Map.copyOf(new LinkedHashMap<>(reacoes));
		this.danoResolvido = danoPorAlvo == null ? Map.of()
				: Map.copyOf(new LinkedHashMap<>(danoPorAlvo));
		this.filaContraAtaques = contraAtaques == null ? List.of() : List.copyOf(contraAtaques);
	}

	public UUID getId() { return id; }
	public Instant getCriadoEm() { return criadoEm; }
	public AcaoMestreInput getInput() { return input; }
	public Personagem getAtor() { return input.getAtor(); }
	public TipoAcao getTipoAcao() { return tipoAcao; }
	public Habilidade getHabilidade() { return habilidade; }
	public int getCustoMana() { return custoMana; }
	public int getCustoTU() { return custoTU; }
	public Status getStatus() { return status; }
	public Map<Personagem, List<DamageEvent>> getDanos() { return previaDano.getDanos(); }
	public Map<String, String> getReacoesResolvidas() { return reacoesResolvidas; }
	public Map<String, Double> getDanoResolvido() { return danoResolvido; }
	public List<String> getFilaContraAtaques() { return filaContraAtaques; }
	public String getNomeAcao() { return habilidade != null ? habilidade.getNome() : "Ataque Básico"; }
}

package br.com.dantesrpg.model.combat;

import br.com.dantesrpg.model.Personagem;
import java.util.List;

/**
 * Contém o resultado de um cálculo de empuxo (knockback).
 * Produzido por {@link KnockbackProcessor} e consumido por
 * {@link DamageApplicator} e por {@code MapController} para renderização.
 */
public class KnockbackResult {

	/** Posição final X (grid) onde o alvo para. */
	private final int posicaoFinalX;

	/** Posição final Y (grid) onde o alvo para. */
	private final int posicaoFinalY;

	/** Distância real percorrida (pode ser menor que a distância calculada). */
	private final int distanciaReal;

	/** true se houve colisão com parede, borda ou entidade antes da distância máxima. */
	private final boolean colidiu;

	/**
	 * A entidade que bloqueou o empuxo, se houver (null se a colisão foi com
	 * parede/borda ou se não houve colisão).
	 */
	private final Personagem colidiuCom;

	/**
	 * Dano de impacto sugerido por tiles "não percorridos".
	 * Calculado como: (distanciaMaxima - distanciaReal) × fatorDanoImpacto.
	 * Zero quando não houve colisão.
	 */
	private final double danoImpacto;

	/**
	 * Lista de coordenadas {x, y} que o alvo percorreria (inclusive a posição
	 * de parada). Usado para desenhar o preview na grid.
	 */
	private final List<int[]> trajetoria;

	/** Direção horizontal do empuxo (-1, 0 ou +1). */
	private final int direcaoX;

	/** Direção vertical do empuxo (-1, 0 ou +1). */
	private final int direcaoY;

	public KnockbackResult(int posicaoFinalX, int posicaoFinalY, int distanciaReal,
			boolean colidiu, Personagem colidiuCom, double danoImpacto,
			List<int[]> trajetoria, int direcaoX, int direcaoY) {
		this.posicaoFinalX = posicaoFinalX;
		this.posicaoFinalY = posicaoFinalY;
		this.distanciaReal = distanciaReal;
		this.colidiu = colidiu;
		this.colidiuCom = colidiuCom;
		this.danoImpacto = danoImpacto;
		this.trajetoria = trajetoria;
		this.direcaoX = direcaoX;
		this.direcaoY = direcaoY;
	}

	// ========== GETTERS ==========

	public int getPosicaoFinalX() { return posicaoFinalX; }
	public int getPosicaoFinalY() { return posicaoFinalY; }
	public int getDistanciaReal()  { return distanciaReal; }
	public boolean isColidiu()     { return colidiu; }
	public Personagem getColidiuCom() { return colidiuCom; }
	public double getDanoImpacto() { return danoImpacto; }
	public List<int[]> getTrajetoria() { return trajetoria; }
	public int getDirecaoX()       { return direcaoX; }
	public int getDirecaoY()       { return direcaoY; }

	/** Atalho: retorna true se o empuxo realmente moveu a entidade ao menos 1 tile. */
	public boolean houveMomento() { return distanciaReal > 0; }
}

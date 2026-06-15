package br.com.dantesrpg.model.enums;

public enum TipoAcao {
	ATAQUE_BASICO, HABILIDADE, ITEM, FANTASMA_NOBRE, MOVIMENTO, PASSAR_VEZ, RECARREGAR, OUTRO, NENHUMA,

	DOT, // Dano por Tempo (Veneno, Sangramento)
	AMBIENTE, // Chuva, Raio, Vento
	REACAO_FANTASMA, // Contra-ataque específico ou Reflexão
	ECO // Dano do Combo do Pugilista
}
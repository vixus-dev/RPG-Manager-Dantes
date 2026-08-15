package br.com.dantesrpg.model.combat;

/** Etapas explícitas do workspace de turno do mestre. */
public enum EtapaFluxoTurno {
	AGUARDANDO_INICIO,
	EFEITOS_PENDENTES,
	ESCOLHENDO_ACAO,
	MOVIMENTANDO,
	SELECIONANDO_ALVOS,
	COLETANDO_DADOS,
	COLETANDO_REACOES,
	REVISANDO,
	APLICANDO,
	CONCLUIDO
}

package br.com.dantesrpg.controller.service;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import br.com.dantesrpg.model.Amuleto;
import br.com.dantesrpg.model.Arma;
import br.com.dantesrpg.model.Armadura;
import br.com.dantesrpg.model.Classe;
import br.com.dantesrpg.model.Grimorio;
import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.ImplementoMagico;
import br.com.dantesrpg.model.Item;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.ArmaMelee;
import br.com.dantesrpg.model.ArmaRanged;
import br.com.dantesrpg.model.classes.ClassePlaceholder;
import br.com.dantesrpg.model.EfeitoOnHit;
import br.com.dantesrpg.model.enums.Atributo;
import br.com.dantesrpg.model.enums.Raridade;
import br.com.dantesrpg.model.enums.TipoAlvo;
import br.com.dantesrpg.model.items.Consumivel;
import br.com.dantesrpg.model.items.EssenciaInimigo;
import br.com.dantesrpg.model.items.PocaoAlquimica;
import br.com.dantesrpg.model.racas.RaçaPlaceholder;
import br.com.dantesrpg.model.util.FileLoader;
import br.com.dantesrpg.model.util.HabilidadeFactory;

public class CatalogoItensService {

	private Map<String, Map<String, Object>> armoryDatabase = new HashMap<>();
	private Map<String, Map<String, Object>> itempediaDatabase = new HashMap<>();

	public void carregarArmaria() {
		this.armoryDatabase = new HashMap<>();
		Gson gson = new Gson();
		String resourcePath = "/data/armas.json";

		try (InputStream is = FileLoader.carregarArquivo(resourcePath)) {
			if (is == null) {
				System.err.println("Erro Crítico: Arquivo da Armaria não encontrado: " + resourcePath);
				return;
			}
			try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
				Type mapType = new TypeToken<Map<String, Map<String, Object>>>() {
				}.getType();
				this.armoryDatabase = gson.fromJson(reader, mapType);
				System.out.println("ARMORIA: Carregada com sucesso.");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void carregarItempedia() {
		this.itempediaDatabase = new HashMap<>();
		Gson gson = new Gson();
		String[] arquivos = { "/data/consumiveis.json", "/data/armaduras.json", "/data/amuletos.json" };

		for (String resourcePath : arquivos) {
			try (InputStream is = FileLoader.carregarArquivo(resourcePath)) {
				if (is == null) {
					continue;
				}

				try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
					Type mapType = new TypeToken<Map<String, Map<String, Object>>>() {
					}.getType();
					Map<String, Map<String, Object>> dados = gson.fromJson(reader, mapType);
					this.itempediaDatabase.putAll(dados);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public Map<String, Map<String, Object>> getArmoryDatabase() {
		return armoryDatabase;
	}

	public Map<String, Map<String, Object>> getItempediaDatabase() {
		return itempediaDatabase;
	}

	public List<String> getListaNomesArmas() {
		if (armoryDatabase == null) {
			return new ArrayList<>();
		}
		return new ArrayList<>(armoryDatabase.keySet());
	}

	public Arma getArma(String nomeArma) {
		if (nomeArma == null || nomeArma.isEmpty()) {
			return null;
		}

		if (nomeArma.equals("Punho Infinito"))
			return new br.com.dantesrpg.model.armas.unicas.PunhoInfinito();
		if (nomeArma.equals("Murasame"))
			return new br.com.dantesrpg.model.armas.unicas.Murasame();
		if (nomeArma.equals("Terrore"))
			return new br.com.dantesrpg.model.armas.unicas.Terrore();
		if (nomeArma.equals("Pálida Vigília"))
			return new br.com.dantesrpg.model.armas.unicas.PalidaVigilia();
		if (nomeArma.equals("Rubrum"))
			return new br.com.dantesrpg.model.armas.unicas.Rubrum();
		if (nomeArma.equals("Laminas Do Exterminio"))
			return new br.com.dantesrpg.model.armas.unicas.LaminasDoExterminio();
		if (nomeArma.equals("Yaweh"))
			return new br.com.dantesrpg.model.armas.unicas.Yaweh();

		if (nomeArma.equals("Bastão Primordial Behemoth"))
			return new br.com.dantesrpg.model.armas.unicas.BastaoPrimordialBehemoth();
		if (nomeArma.equals("Espada-Serra"))
			return new br.com.dantesrpg.model.armas.boss.EspadaSerra();
		if (nomeArma.equals("LivroDeMalkaresh"))
			return new br.com.dantesrpg.model.armas.boss.LivroDeMalkaresh();
		if (nomeArma.equals("Justiça & Esplendor"))
			return new br.com.dantesrpg.model.armas.boss.JusticaESplendor();

		if (armoryDatabase.containsKey(nomeArma)) {
			Map<String, Object> armaData = armoryDatabase.get(nomeArma);
			return mapearArma(armaData);
		}

		System.err.println("FÁBRICA DE ARMAS: Arma '" + nomeArma + "' não encontrada em .java ou .json!");
		return null;
	}

	public Item getItem(String tipoItem, Map<String, Map<String, Object>> bestiarioDatabase) {
		if (tipoItem == null)
			return null;

		if (tipoItem.startsWith("PocaoAlquimica_")) {
			String[] parts = tipoItem.split("_");
			if (parts.length >= 3) {
				String tipoPocao = parts[1];
				int is = 10;
				try {
					is = Integer.parseInt(parts[2]);
				} catch (NumberFormatException e) {
					e.printStackTrace();
				}
				boolean isV2 = tipoItem.contains("_V2_");
				int roll = 0;
				if (isV2 && parts.length >= 5) {
					try {
						roll = Integer.parseInt(parts[4]);
					} catch (NumberFormatException e) {
						e.printStackTrace();
					}
				}
				return new PocaoAlquimica(tipoItem, tipoPocao, is, isV2, roll);
			}
		}

		if (itempediaDatabase != null && itempediaDatabase.containsKey(tipoItem)) {
			try {
				Map<String, Object> data = itempediaDatabase.get(tipoItem);
				String classeItem = (String) data.getOrDefault("classeItem", "Consumivel");
				String descricao = (String) data.getOrDefault("descricao", "Sem descrição.");
				String tipoMoeda = (String) data.getOrDefault("tipoMoeda", "BRONZE");
				int valor = ((Double) data.getOrDefault("valorMoedas", 0.0)).intValue();

				String nome = (String) data.getOrDefault("nome", tipoItem);
				Map<String, Double> modStatus = parseModificadoresStatus(
						(Map<String, Double>) data.get("modificadoresStatus"));

				if ("Consumivel".equalsIgnoreCase(classeItem)) {
					int custoTU = ((Double) data.getOrDefault("custoTU", 100.0)).intValue();
					boolean usavel = (Boolean) data.getOrDefault("usavelEmCombate", false);
					Map<String, Double> efeitos = (Map<String, Double>) data.get("efeitos");
					Consumivel consumivel = new Consumivel(tipoItem, nome, descricao, valor, custoTU, usavel, efeitos);
					consumivel.setTipoMoeda(tipoMoeda);
					return consumivel;
				}

				if ("Armadura".equalsIgnoreCase(classeItem)) {
					return mapearArmadura(data);
				}

				if ("Amuleto".equalsIgnoreCase(classeItem)) {
					return mapearAmuleto(data);
				}
			} catch (Exception e) {
				System.err.println("Erro ao mapear Item da Itempedia: " + tipoItem);
				e.printStackTrace();
				return null;
			}
		}

		if (tipoItem.startsWith("Essência de ")) {
			return criarEssencia(tipoItem, bestiarioDatabase);
		}

		Item itemComoEquipamento = getArma(tipoItem);
		if (itemComoEquipamento != null) {
			return itemComoEquipamento;
		}

		System.err.println("FÁBRICA DE ITENS: Tipo de item desconhecido: " + tipoItem);
		return null;
	}

	private Item criarEssencia(String tipoItem, Map<String, Map<String, Object>> bestiarioDatabase) {
		String nomeSujo = tipoItem.substring("Essência de ".length());
		String nomeMonstroAlvo = tipoItem.substring("Essência de ".length());
		String nomeLimpo = nomeSujo.replaceAll("[0-9]+$", "").trim();
		System.out.println("DEBUG ESSÊNCIA: Sujo='" + nomeSujo + "' -> Limpo='" + nomeLimpo + "'");
		Map<String, Object> dadosMonstro = null;

		if (bestiarioDatabase != null) {
			for (Map.Entry<String, Map<String, Object>> entry : bestiarioDatabase.entrySet()) {
				String nomeNoBestiario = (String) entry.getValue().get("nome");
				if (nomeNoBestiario != null && nomeNoBestiario.trim().equalsIgnoreCase(nomeLimpo)) {
					dadosMonstro = entry.getValue();
					break;
				}
			}
		}

		if (dadosMonstro != null) {
			int vida = ((Double) dadosMonstro.getOrDefault("vida", 10.0)).intValue();
			int def = ((Double) dadosMonstro.getOrDefault("defesa", 0.0)).intValue();
			String nomeArma = (String) dadosMonstro.getOrDefault("arma", null);

			Map<Atributo, Integer> atr = new HashMap<>();
			for (Atributo atributo : Atributo.values()) {
				atr.put(atributo, 1);
			}
			atr.put(Atributo.DESTREZA, def);
			atr.put(Atributo.TOPOR, def);

			Classe classe = new ClassePlaceholder();
			Personagem dummy = new Personagem(nomeMonstroAlvo, new RaçaPlaceholder(), classe, 1, atr, vida, 0);
			if (nomeArma != null) {
				dummy.setArmaEquipada(getArma(nomeArma));
			}

			return new EssenciaInimigo(dummy);
		}

		System.err.println(
				"Aviso: Essência de '" + nomeMonstroAlvo + "' não encontrada no bestiário. Criando genérica.");
		Personagem dummyGenerico = new Personagem(nomeMonstroAlvo, new RaçaPlaceholder(),
				new ClassePlaceholder(), 1, new HashMap<>(), 10, 0);
		return new EssenciaInimigo(dummyGenerico);
	}

	public Arma mapearArmaEquipadaSalva(Object armaInfo) {
		if (armaInfo instanceof String) {
			return getArma((String) armaInfo);
		}
		if (!(armaInfo instanceof Map)) {
			return null;
		}

		Map<String, Object> armaData = (Map<String, Object>) armaInfo;
		String nomeArma = (String) armaData.get("nome");
		Arma arma = getArma(nomeArma);
		if (arma == null) {
			return null;
		}

		if (armaData.containsKey("overclock")) {
			arma.setGrauOverclock(((Number) armaData.get("overclock")).intValue());
		}

		if (arma instanceof Grimorio && armaData.containsKey("magiasSalvas")) {
			Grimorio grimorio = (Grimorio) arma;
			grimorio.getMagiasArmazenadas().clear();
			List<String> magiasSalvas = (List<String>) armaData.get("magiasSalvas");
			for (String nomeMagia : magiasSalvas) {
				Habilidade habilidade = HabilidadeFactory.criarHabilidadePorNome(nomeMagia);
				if (habilidade != null) {
					grimorio.aprenderMagia(habilidade);
				}
			}
			System.out.println("GRIMORIO CARREGADO: " + nomeArma + " com " + magiasSalvas.size()
					+ " magias customizadas.");
		}

		return arma;
	}

	public Object criarDadosArmaEquipada(Arma arma) {
		if (arma == null) {
			return null;
		}
		if (!(arma instanceof Grimorio) && arma.getGrauOverclock() <= 0) {
			return arma.getNome();
		}

		Map<String, Object> armaData = new HashMap<>();
		armaData.put("nome", arma.getNome());
		if (arma.getGrauOverclock() > 0) {
			armaData.put("overclock", arma.getGrauOverclock());
		}
		if (arma instanceof Grimorio) {
			Grimorio grimorio = (Grimorio) arma;
			List<String> nomesMagias = grimorio.getMagiasArmazenadas().stream()
					.map(Habilidade::getNome)
					.collect(Collectors.toList());
			armaData.put("magiasSalvas", nomesMagias);
		}
		return armaData;
	}

	public Arma mapearArma(Map<String, Object> armaData) {
		try {
			String nome = (String) armaData.getOrDefault("nome", "Arma Desconhecida");
			String categoria = (String) armaData.getOrDefault("categoria", "Desconhecida");
			String descricao = (String) armaData.getOrDefault("descricao", "Uma arma.");
			String tipoMoeda = (String) armaData.getOrDefault("tipoMoeda", "BRONZE");
			Raridade raridade = Raridade.valueOf(((String) armaData.getOrDefault("raridade", "COMUM")).toUpperCase());
			int danoBase = ((Double) armaData.getOrDefault("danoBase", 1.0)).intValue();
			int valorMoedas = ((Double) armaData.getOrDefault("valorMoedas", 0.0)).intValue();
			int ticks = ((Double) armaData.getOrDefault("ticksDeDano", 1.0)).intValue();
			Atributo atributo = Atributo
					.valueOf(((String) armaData.getOrDefault("atributoMultiplicador", "FORCA")).toUpperCase());
			int custoTU = ((Double) armaData.getOrDefault("custoTU", 100.0)).intValue();
			int wielding = ((Double) armaData.getOrDefault("wielding", 1.0)).intValue();

			List<EfeitoOnHit> efeitosOnHit = parseEfeitosOnHit(armaData);

			int alcanceJson = -1;
			if (armaData.containsKey("alcance")) {
				alcanceJson = ((Double) armaData.get("alcance")).intValue();
			}

			String tipo = (String) armaData.getOrDefault("tipo", "Melee");
			Map<String, Double> modsJson = (Map<String, Double>) armaData.get("modificadoresDeAtributo");
			Map<Atributo, Integer> modsFinais = null;
			if (modsJson != null) {
				modsFinais = new HashMap<>();
				for (Map.Entry<String, Double> entry : modsJson.entrySet()) {
					try {
						modsFinais.put(Atributo.valueOf(entry.getKey().toUpperCase()), entry.getValue().intValue());
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}

			Map<String, Double> modsStatusJson = (Map<String, Double>) armaData.get("modificadoresStatus");
			Map<String, Double> modsStatusFinais = parseModificadoresStatus(modsStatusJson);

			Arma armaFinal = null;
			if ("Melee".equalsIgnoreCase(tipo)) {
				int alcanceFinal = (alcanceJson != -1) ? alcanceJson : 1;
				armaFinal = new ArmaMelee(nome, categoria, descricao, raridade, valorMoedas,
						danoBase, ticks, atributo, custoTU, alcanceFinal);
			} else if ("Ranged".equalsIgnoreCase(tipo)) {
				int alcanceFinal = (alcanceJson != -1) ? alcanceJson : 5;
				int municao = ((Double) armaData.getOrDefault("municaoMaxima", 6.0)).intValue();
				armaFinal = new ArmaRanged(nome, categoria, descricao, raridade, valorMoedas,
						danoBase, ticks, atributo, custoTU, alcanceFinal, municao);
			} else if ("Magico".equalsIgnoreCase(tipo)) {
				int alcanceFinal = (alcanceJson != -1) ? alcanceJson : 3;
				armaFinal = new ImplementoMagico(nome, categoria, descricao, raridade,
						valorMoedas, danoBase, atributo, custoTU, alcanceFinal);
			} else if ("Grimorio".equalsIgnoreCase(tipo)) {
				int maxSlots = ((Double) armaData.getOrDefault("maxSlots", 3.0)).intValue();
				int alcanceFinal = (alcanceJson != -1) ? alcanceJson : 4;

				Grimorio grimorio = new Grimorio(nome, categoria, descricao, raridade, valorMoedas, danoBase, atributo,
						custoTU, alcanceFinal, maxSlots);
				List<String> nomesMagias = (List<String>) armaData.get("magias");
				if (nomesMagias != null) {
					for (String nomeMagia : nomesMagias) {
						Habilidade habilidade = HabilidadeFactory.criarHabilidadePorNome(nomeMagia);
						if (habilidade != null) {
							grimorio.aprenderMagia(habilidade);
						}
					}
				}
				armaFinal = grimorio;
			}

			if (armaFinal != null) {
				armaFinal.setEfeitosOnHit(efeitosOnHit);

				String habilidadeUnica = (String) armaData.getOrDefault("habilidadeConcedida", null);
				if (habilidadeUnica != null) {
					armaFinal.addHabilidadeConcedida(habilidadeUnica);
				}

				List<String> listaHabilidades = (List<String>) armaData.get("habilidadesConcedidas");
				if (listaHabilidades != null) {
					for (String habilidade : listaHabilidades) {
						armaFinal.addHabilidadeConcedida(habilidade);
					}
				}

				try {
					String tipoAlvoStr = (String) armaData.getOrDefault("tipoAlvo", "INDIVIDUAL");
					armaFinal.setTipoAlvo(TipoAlvo.valueOf(tipoAlvoStr.toUpperCase()));
				} catch (Exception e) {
				}

				int tamanhoArea = ((Double) armaData.getOrDefault("tamanhoArea", 0.0)).intValue();
				armaFinal.setTamanhoArea(tamanhoArea);

				armaFinal.setTipoMoeda(tipoMoeda);
				armaFinal.setWielding(wielding);
				armaFinal.setModificadoresDeAtributo(modsFinais);
				armaFinal.setModificadoresStatus(modsStatusFinais);

				// --- Ataque Especial ---
				Boolean hasSpecial = (Boolean) armaData.getOrDefault("HasSpecialAttack", false);
				if (hasSpecial != null && hasSpecial) {
					armaFinal.setHasSpecialAttack(true);

					String specialName = (String) armaData.getOrDefault("SpecialAttackName", null);
					if (specialName != null && !specialName.isEmpty()) {
						armaFinal.setSpecialAttackName(specialName);
					} else {
						armaFinal.setSpecialAttackName("Ataque Especial");
					}

					Object dmgObj = armaData.get("SpecialAttackDMG");
					if (dmgObj instanceof Number) {
						armaFinal.setSpecialAttackDmg(((Number) dmgObj).doubleValue());
					}

					Object cdObj = armaData.get("SpecialAttackCD");
					if (cdObj instanceof Number) {
						armaFinal.setSpecialAttackCd(((Number) cdObj).doubleValue());
					}

					String specialType = (String) armaData.getOrDefault("SpecialAttackType", "INDIVIDUAL");
					armaFinal.setSpecialAttackType(specialType);

					Object sizeObj = armaData.get("specialAttackSize");
					if (sizeObj instanceof Number) {
						armaFinal.setSpecialAttackSize(((Number) sizeObj).intValue());
					}

					System.out.println("ARMORIA: Arma '" + nome + "' possui Ataque Especial: "
							+ armaFinal.getSpecialAttackName());
				}

				return armaFinal;
			}

			System.err.println("Tipo de arma não reconhecido: " + tipo);
			return null;
		} catch (Exception e) {
			System.err.println("Erro ao mapear dados da arma do JSON:");
			e.printStackTrace();
			return null;
		}
	}

	public Armadura mapearArmadura(Map<String, Object> data) {
		if (data == null)
			return null;
		try {
			String nome = (String) data.getOrDefault("nome", "Armadura Desconhecida");
			String descricao = (String) data.getOrDefault("descricao", "Uma armadura resistente.");
			String tipoMoeda = (String) data.getOrDefault("tipoMoeda", "BRONZE");
			int valor = data.containsKey("valorMoedas") ? ((Double) data.get("valorMoedas")).intValue() : 0;
			int armaduraBase = ((Double) data.getOrDefault("armaduraBase", 0.0)).intValue();

			Map<Atributo, Integer> modificadores = parseModificadores(
					(Map<String, Double>) data.get("modificadoresDeAtributo"));
			Map<String, Double> modStatus = parseModificadoresStatus(
					(Map<String, Double>) data.get("modificadoresStatus"));
			Armadura armadura = new Armadura(nome, descricao, valor, armaduraBase, modificadores, modStatus);
			armadura.setTipoMoeda(tipoMoeda);

			if (data.containsKey("efeitoAoTomarDano")) {
				armadura.setNomeEfeitoOnDamageTaken((String) data.get("efeitoAoTomarDano"));
			}
			if (data.containsKey("chanceEfeitoAoTomarDano")) {
				armadura.setChanceEfeitoOnDamageTaken(((Number) data.get("chanceEfeitoAoTomarDano")).doubleValue());
			}
			if (data.containsKey("alvoEfeitoAoTomarDano")) {
				armadura.setAlvoEfeitoOnDamageTaken((String) data.get("alvoEfeitoAoTomarDano"));
			}

			if (data.containsKey("habilidadesConcedidas")) {
				List<String> habs = (List<String>) data.get("habilidadesConcedidas");
				if (habs != null) {
					for (String h : habs) {
						armadura.addHabilidadeConcedida(h);
					}
				}
			}

			return armadura;
		} catch (Exception e) {
			System.err.println("Erro ao mapear dados da Armadura do JSON:");
			e.printStackTrace();
			return null;
		}
	}

	public Amuleto mapearAmuleto(Map<String, Object> data) {
		if (data == null)
			return null;
		try {
			String nome = (String) data.getOrDefault("nome", "Amuleto Desconhecido");
			String descricao = (String) data.getOrDefault("descricao", "Sem descrição.");
			String tipoMoeda = (String) data.getOrDefault("tipoMoeda", "BRONZE");
			int valor = data.containsKey("valorMoedas") ? ((Double) data.get("valorMoedas")).intValue() : 0;
			int armaduraBonus = ((Double) data.getOrDefault("armaduraBonus", 0.0)).intValue();

			Map<Atributo, Integer> modificadores = parseModificadores(
					(Map<String, Double>) data.get("modificadoresDeAtributo"));
			Map<String, Double> modStatus = parseModificadoresStatus(
					(Map<String, Double>) data.get("modificadoresStatus"));
			Amuleto amuleto = new Amuleto(nome, descricao, valor, armaduraBonus, modificadores, modStatus);
			amuleto.setTipoMoeda(tipoMoeda);

			if (data.containsKey("habilidadesConcedidas")) {
				List<String> habs = (List<String>) data.get("habilidadesConcedidas");
				if (habs != null) {
					for (String h : habs) {
						amuleto.addHabilidadeConcedida(h);
					}
				}
			}

			return amuleto;
		} catch (Exception e) {
			System.err.println("Erro ao mapear dados do Amuleto do JSON:");
			e.printStackTrace();
			return null;
		}
	}

	public Map<String, Double> parseModificadoresStatus(Map<String, Double> input) {
		if (input == null)
			return new HashMap<>();
		return new HashMap<>(input);
	}

	private List<EfeitoOnHit> parseEfeitosOnHit(Map<String, Object> armaData) {
		List<EfeitoOnHit> efeitos = new ArrayList<>();
		Object efeitosNovos = armaData.get("efeitosAoAcertar");

		if (efeitosNovos instanceof List<?>) {
			for (Object item : (List<?>) efeitosNovos) {
				if (!(item instanceof Map<?, ?>)) {
					continue;
				}
				Map<?, ?> efeitoData = (Map<?, ?>) item;
				Object nomeObj = efeitoData.containsKey("efeito") ? efeitoData.get("efeito") : efeitoData.get("nome");
				Object chanceObj = efeitoData.get("chance");

				if (nomeObj instanceof String && chanceObj instanceof Number) {
					efeitos.add(new EfeitoOnHit((String) nomeObj, ((Number) chanceObj).doubleValue()));
				}
			}
			return efeitos;
		}

		String efeitoLegado = (String) armaData.getOrDefault("efeitoAoAcertar", null);
		Object chanceObj = armaData.get("chanceEfeito");
		if (efeitoLegado != null && !efeitoLegado.isBlank() && chanceObj instanceof Number) {
			efeitos.add(new EfeitoOnHit(efeitoLegado, ((Number) chanceObj).doubleValue()));
		}
		return efeitos;
	}

	public Arma criarCopiaDaArma(Arma original) {
		if (original == null)
			return null;

		Arma copia = getArma(original.getNome());
		if (copia == null)
			return null;

		if (copia.isRequerMunicao()) {
			copia.recarregar();
		}

		if (original instanceof Grimorio && copia instanceof Grimorio) {
			Grimorio originalGrimorio = (Grimorio) original;
			Grimorio copiaGrimorio = (Grimorio) copia;
			copiaGrimorio.getMagiasArmazenadas().clear();
			copiaGrimorio.getMagiasArmazenadas().addAll(originalGrimorio.getMagiasArmazenadas());
		}

		return copia;
	}

	private Map<Atributo, Integer> parseModificadores(Map<String, Double> modsJson) {
		Map<Atributo, Integer> mods = new HashMap<>();
		if (modsJson != null) {
			for (Map.Entry<String, Double> entry : modsJson.entrySet()) {
				try {
					mods.put(Atributo.valueOf(entry.getKey().toUpperCase()), entry.getValue().intValue());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return mods;
	}
}

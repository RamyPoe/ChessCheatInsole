package nl.s22k.chess.move;

import nl.s22k.chess.Util;

public final class MagicUtil {

	// rook-size: 800kb
	// bishop-size: 40kb

	// TODO smaller tables?
	private static final long[] rookMagicNumbers = { 0xa180022080400230L, 0x40100040022000L, 0x80088020001002L, 0x80080280841000L, 0x4200042010460008L,
			0x4800a0003040080L, 0x400110082041008L, 0x8000a041000880L, 0x10138001a080c010L, 0x804008200480L, 0x10011012000c0L, 0x22004128102200L,
			0x200081201200cL, 0x202a001048460004L, 0x81000100420004L, 0x4000800380004500L, 0x208002904001L, 0x90004040026008L, 0x208808010002001L,
			0x2002020020704940L, 0x8048010008110005L, 0x6820808004002200L, 0xa80040008023011L, 0xb1460000811044L, 0x4204400080008ea0L, 0xb002400180200184L,
			0x2020200080100380L, 0x10080080100080L, 0x2204080080800400L, 0xa40080360080L, 0x2040604002810b1L, 0x8c218600004104L, 0x8180004000402000L,
			0x488c402000401001L, 0x4018a00080801004L, 0x1230002105001008L, 0x8904800800800400L, 0x42000c42003810L, 0x8408110400b012L, 0x18086182000401L,
			0x2240088020c28000L, 0x1001201040c004L, 0xa02008010420020L, 0x10003009010060L, 0x4008008008014L, 0x80020004008080L, 0x282020001008080L,
			0x50000181204a0004L, 0x102042111804200L, 0x40002010004001c0L, 0x19220045508200L, 0x20030010060a900L, 0x8018028040080L, 0x88240002008080L,
			0x10301802830400L, 0x332a4081140200L, 0x8080010a601241L, 0x1008010400021L, 0x4082001007241L, 0x211009001200509L, 0x8015001002441801L,
			0x801000804000603L, 0xc0900220024a401L, 0x1000200608243L };
	private static final long[] bishopMagicNumbers = { 0x2910054208004104L, 0x2100630a7020180L, 0x5822022042000000L, 0x2ca804a100200020L, 0x204042200000900L,
			0x2002121024000002L, 0x80404104202000e8L, 0x812a020205010840L, 0x8005181184080048L, 0x1001c20208010101L, 0x1001080204002100L, 0x1810080489021800L,
			0x62040420010a00L, 0x5028043004300020L, 0xc0080a4402605002L, 0x8a00a0104220200L, 0x940000410821212L, 0x1808024a280210L, 0x40c0422080a0598L,
			0x4228020082004050L, 0x200800400e00100L, 0x20b001230021040L, 0x90a0201900c00L, 0x4940120a0a0108L, 0x20208050a42180L, 0x1004804b280200L,
			0x2048020024040010L, 0x102c04004010200L, 0x20408204c002010L, 0x2411100020080c1L, 0x102a008084042100L, 0x941030000a09846L, 0x244100800400200L,
			0x4000901010080696L, 0x280404180020L, 0x800042008240100L, 0x220008400088020L, 0x4020182000904c9L, 0x23010400020600L, 0x41040020110302L,
			0x412101004020818L, 0x8022080a09404208L, 0x1401210240484800L, 0x22244208010080L, 0x1105040104000210L, 0x2040088800c40081L, 0x8184810252000400L,
			0x4004610041002200L, 0x40201a444400810L, 0x4611010802020008L, 0x80000b0401040402L, 0x20004821880a00L, 0x8200002022440100L, 0x9431801010068L,
			0x1040c20806108040L, 0x804901403022a40L, 0x2400202602104000L, 0x208520209440204L, 0x40c000022013020L, 0x2000104000420600L, 0x400000260142410L,
			0x800633408100500L, 0x2404080a1410L, 0x138200122002900L };

	private static final Magic[] rookMagics = new Magic[64];
	private static final Magic[] bishopMagics = new Magic[64];

	public static long getRookMoves(final int fromIndex, final long allPieces) {
		final Magic magic = rookMagics[fromIndex];
		return magic.magicMoves[(int) ((allPieces & magic.movementMask) * magic.magicNumber >>> magic.shift)];
	}

	public static long getBishopMoves(final int fromIndex, final long allPieces) {
		final Magic magic = bishopMagics[fromIndex];
		return magic.magicMoves[(int) ((allPieces & magic.movementMask) * magic.magicNumber >>> magic.shift)];
	}

	public static long getQueenMoves(final int fromIndex, final long allPieces) {
		final Magic rookMagic = rookMagics[fromIndex];
		final Magic bishopMagic = bishopMagics[fromIndex];
		return rookMagic.magicMoves[(int) ((allPieces & rookMagic.movementMask) * rookMagic.magicNumber >>> rookMagic.shift)]
				| bishopMagic.magicMoves[(int) ((allPieces & bishopMagic.movementMask) * bishopMagic.magicNumber >>> bishopMagic.shift)];
	}

	public static long getRookMovesEmptyBoard(final int fromIndex) {
		return rookMagics[fromIndex].magicMoves[0];
	}

	public static long getBishopMovesEmptyBoard(final int fromIndex) {
		return bishopMagics[fromIndex].magicMoves[0];
	}

	public static long getQueenMovesEmptyBoard(final int fromIndex) {
		return bishopMagics[fromIndex].magicMoves[0] | rookMagics[fromIndex].magicMoves[0];
	}

	static {
		for (int i = 0; i < 64; i++) {
			rookMagics[i] = new Magic(rookMagicNumbers[i]);
			bishopMagics[i] = new Magic(bishopMagicNumbers[i]);
		}
		calculateBishopMovementMasks();
		calculateRookMovementMasks();
		generateShiftArrys();
		long[][] bishopOccupancyVariations = calculateVariations(bishopMagics);
		long[][] rookOccupancyVariations = calculateVariations(rookMagics);
		generateBishopMoveDatabase(bishopOccupancyVariations);
		generateRookMoveDatabase(rookOccupancyVariations);
	}

	private static void generateShiftArrys() {
		for (int i = 0; i < 64; i++) {
			rookMagics[i].shift = 64 - Long.bitCount(rookMagics[i].movementMask);
			bishopMagics[i].shift = 64 - Long.bitCount(bishopMagics[i].movementMask);
		}
	}

	private static long[][] calculateVariations(Magic[] magics) {

		long[][] occupancyVariations = new long[64][];
		for (int index = 0; index < 64; index++) {
			int variationCount = (int) Util.POWER_LOOKUP[Long.bitCount(magics[index].movementMask)];
			occupancyVariations[index] = new long[variationCount];

			for (int variationIndex = 1; variationIndex < variationCount; variationIndex++) {
				long currentMask = magics[index].movementMask;

				for (int i = 0; i < 32 - Integer.numberOfLeadingZeros(variationIndex); i++) {
					if ((Util.POWER_LOOKUP[i] & variationIndex) != 0) {
						occupancyVariations[index][variationIndex] |= Long.lowestOneBit(currentMask);
					}
					currentMask &= currentMask - 1;
				}
			}
		}

		return occupancyVariations;
	}

	private static void calculateRookMovementMasks() {
		for (int index = 0; index < 64; index++) {

			// up
			for (int j = index + 8; j < 64 - 8; j += 8) {
				rookMagics[index].movementMask |= Util.POWER_LOOKUP[j];
			}
			// down
			for (int j = index - 8; j >= 0 + 8; j -= 8) {
				rookMagics[index].movementMask |= Util.POWER_LOOKUP[j];
			}
			// left
			for (int j = index + 1; j % 8 != 0 && j % 8 != 7; j++) {
				rookMagics[index].movementMask |= Util.POWER_LOOKUP[j];
			}
			// right
			for (int j = index - 1; j % 8 != 7 && j % 8 != 0 && j > 0; j--) {
				rookMagics[index].movementMask |= Util.POWER_LOOKUP[j];
			}
		}
	}

	private static void calculateBishopMovementMasks() {
		for (int index = 0; index < 64; index++) {

			// up-right
			for (int j = index + 7; j < 64 - 7 && j % 8 != 7 && j % 8 != 0; j += 7) {
				bishopMagics[index].movementMask |= Util.POWER_LOOKUP[j];
			}
			// up-left
			for (int j = index + 9; j < 64 - 9 && j % 8 != 7 && j % 8 != 0; j += 9) {
				bishopMagics[index].movementMask |= Util.POWER_LOOKUP[j];
			}
			// down-right
			for (int j = index - 9; j >= 0 + 9 && j % 8 != 7 && j % 8 != 0; j -= 9) {
				bishopMagics[index].movementMask |= Util.POWER_LOOKUP[j];
			}
			// down-left
			for (int j = index - 7; j >= 0 + 7 && j % 8 != 7 && j % 8 != 0; j -= 7) {
				bishopMagics[index].movementMask |= Util.POWER_LOOKUP[j];
			}
		}
	}

	private static void generateRookMoveDatabase(long[][] rookOccupancyVariations) {
		for (int index = 0; index < 64; index++) {
			rookMagics[index].magicMoves = new long[rookOccupancyVariations[index].length];
			for (int variationIndex = 0; variationIndex < rookOccupancyVariations[index].length; variationIndex++) {
				long validMoves = 0;
				int magicIndex = (int) ((rookOccupancyVariations[index][variationIndex] * rookMagicNumbers[index]) >>> rookMagics[index].shift);

				for (int j = index + 8; j < 64; j += 8) {
					validMoves |= Util.POWER_LOOKUP[j];
					if ((rookOccupancyVariations[index][variationIndex] & Util.POWER_LOOKUP[j]) != 0) {
						break;
					}
				}
				for (int j = index - 8; j >= 0; j -= 8) {
					validMoves |= Util.POWER_LOOKUP[j];
					if ((rookOccupancyVariations[index][variationIndex] & Util.POWER_LOOKUP[j]) != 0) {
						break;
					}
				}
				for (int j = index + 1; j % 8 != 0; j++) {
					validMoves |= Util.POWER_LOOKUP[j];
					if ((rookOccupancyVariations[index][variationIndex] & Util.POWER_LOOKUP[j]) != 0) {
						break;
					}
				}
				for (int j = index - 1; j % 8 != 7 && j >= 0; j--) {
					validMoves |= Util.POWER_LOOKUP[j];
					if ((rookOccupancyVariations[index][variationIndex] & Util.POWER_LOOKUP[j]) != 0) {
						break;
					}
				}

				rookMagics[index].magicMoves[magicIndex] = validMoves;
			}
		}
	}

	private static void generateBishopMoveDatabase(long[][] bishopOccupancyVariations) {
		for (int index = 0; index < 64; index++) {
			bishopMagics[index].magicMoves = new long[bishopOccupancyVariations[index].length];
			for (int variationIndex = 0; variationIndex < bishopOccupancyVariations[index].length; variationIndex++) {
				long validMoves = 0;
				int magicIndex = (int) ((bishopOccupancyVariations[index][variationIndex] * bishopMagicNumbers[index]) >>> bishopMagics[index].shift);

				// up-right
				for (int j = index + 7; j % 8 != 7 && j < 64; j += 7) {
					validMoves |= Util.POWER_LOOKUP[j];
					if ((bishopOccupancyVariations[index][variationIndex] & Util.POWER_LOOKUP[j]) != 0) {
						break;
					}
				}
				// up-left
				for (int j = index + 9; j % 8 != 0 && j < 64; j += 9) {
					validMoves |= Util.POWER_LOOKUP[j];
					if ((bishopOccupancyVariations[index][variationIndex] & Util.POWER_LOOKUP[j]) != 0) {
						break;
					}
				}
				// down-right
				for (int j = index - 9; j % 8 != 7 && j >= 0; j -= 9) {
					validMoves |= Util.POWER_LOOKUP[j];
					if ((bishopOccupancyVariations[index][variationIndex] & Util.POWER_LOOKUP[j]) != 0) {
						break;
					}
				}
				// down-left
				for (int j = index - 7; j % 8 != 0 && j >= 0; j -= 7) {
					validMoves |= Util.POWER_LOOKUP[j];
					if ((bishopOccupancyVariations[index][variationIndex] & Util.POWER_LOOKUP[j]) != 0) {
						break;
					}
				}

				bishopMagics[index].magicMoves[magicIndex] = validMoves;
			}
		}
	}

}

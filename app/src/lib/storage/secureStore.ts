import { openDB, type IDBPDatabase } from 'idb';
import { decrypt, encrypt, type EncryptedBlob } from './crypto';

const DB_NAME = 'monnhuitne';
const STORE_NAME = 'secure-config';
const RECORD_KEY = 'n8n-credentials';

export interface StoredCredentials {
	backendBaseUrl: string;
	n8nProxyBaseUrl: string;
	apiKey: string;
}

let dbPromise: Promise<IDBPDatabase> | undefined;

function getDb(): Promise<IDBPDatabase> {
	if (!dbPromise) {
		dbPromise = openDB(DB_NAME, 1, {
			upgrade(db) {
				db.createObjectStore(STORE_NAME);
			}
		});
	}
	return dbPromise;
}

export async function hasStoredCredentials(): Promise<boolean> {
	const db = await getDb();
	const record = await db.get(STORE_NAME, RECORD_KEY);
	return record !== undefined;
}

export async function saveCredentials(credentials: StoredCredentials, pin: string): Promise<void> {
	const blob = await encrypt(JSON.stringify(credentials), pin);
	const db = await getDb();
	await db.put(STORE_NAME, blob, RECORD_KEY);
}

/** Lève une erreur si le PIN est incorrect (échec de l'auth-tag AES-GCM). */
export async function unlockCredentials(pin: string): Promise<StoredCredentials> {
	const db = await getDb();
	const blob = (await db.get(STORE_NAME, RECORD_KEY)) as EncryptedBlob | undefined;
	if (!blob) throw new Error('Aucune configuration enregistrée sur cet appareil.');

	const json = await decrypt(blob, pin);
	return JSON.parse(json) as StoredCredentials;
}

export async function clearCredentials(): Promise<void> {
	const db = await getDb();
	await db.delete(STORE_NAME, RECORD_KEY);
}

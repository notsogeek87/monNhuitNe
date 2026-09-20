// Chiffrement AES-GCM avec clé dérivée d'un PIN via PBKDF2 (WebCrypto natif,
// aucune dépendance). Le PIN n'est jamais stocké : seule la clé dérivée
// (non exportable, en mémoire) sert à déchiffrer le blob IndexedDB.

const PBKDF2_ITERATIONS = 210_000;
const SALT_BYTES = 16;
const IV_BYTES = 12;

export interface EncryptedBlob {
	ciphertext: ArrayBuffer;
	salt: ArrayBuffer;
	iv: ArrayBuffer;
}

async function deriveKey(pin: string, salt: BufferSource): Promise<CryptoKey> {
	const material = await crypto.subtle.importKey('raw', new TextEncoder().encode(pin), 'PBKDF2', false, [
		'deriveKey'
	]);

	return crypto.subtle.deriveKey(
		{ name: 'PBKDF2', salt, iterations: PBKDF2_ITERATIONS, hash: 'SHA-256' },
		material,
		{ name: 'AES-GCM', length: 256 },
		false,
		['encrypt', 'decrypt']
	);
}

export async function encrypt(plaintext: string, pin: string): Promise<EncryptedBlob> {
	const salt = crypto.getRandomValues(new Uint8Array(SALT_BYTES));
	const iv = crypto.getRandomValues(new Uint8Array(IV_BYTES));
	const key = await deriveKey(pin, salt);

	const ciphertext = await crypto.subtle.encrypt({ name: 'AES-GCM', iv }, key, new TextEncoder().encode(plaintext));

	return { ciphertext, salt: salt.buffer, iv: iv.buffer };
}

export async function decrypt(blob: EncryptedBlob, pin: string): Promise<string> {
	const key = await deriveKey(pin, blob.salt);
	const plaintext = await crypto.subtle.decrypt({ name: 'AES-GCM', iv: blob.iv }, key, blob.ciphertext);
	return new TextDecoder().decode(plaintext);
}

<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { hasStoredCredentials, unlockCredentials } from '../storage/secureStore';
	import { unlockedCredentials } from '../stores/session';

	let checking = true;
	let needsUnlock = false;
	let pin = '';
	let error = '';
	let unlocking = false;

	function autofocus(node: HTMLElement) {
		node.focus();
	}

	onMount(async () => {
		if (!(await hasStoredCredentials())) {
			checking = false;
			goto('/settings');
			return;
		}
		needsUnlock = true;
		checking = false;
	});

	async function submit(event: SubmitEvent) {
		event.preventDefault();
		unlocking = true;
		error = '';
		try {
			const credentials = await unlockCredentials(pin);
			unlockedCredentials.set(credentials);
			needsUnlock = false;
		} catch {
			error = 'PIN incorrect.';
		} finally {
			unlocking = false;
			pin = '';
		}
	}
</script>

{#if checking}
	<!-- rien à afficher pendant la vérification IndexedDB -->
{:else if needsUnlock}
	<form class="card" on:submit={submit}>
		<label>
			<span class="muted">PIN de déverrouillage</span>
			<input type="password" inputmode="numeric" autocomplete="off" bind:value={pin} use:autofocus />
		</label>
		{#if error}<p class="error-text">{error}</p>{/if}
		<button class="btn" type="submit" disabled={unlocking}>Déverrouiller</button>
	</form>
{:else}
	<slot />
{/if}

<style>
	form {
		display: flex;
		flex-direction: column;
		gap: 12px;
		margin-top: 20vh;
	}
	.error-text {
		color: var(--error);
		font-size: 0.85rem;
		margin: -4px 0 0;
	}
</style>

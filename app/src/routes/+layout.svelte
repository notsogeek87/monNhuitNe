<script lang="ts">
	import '../app.css';
	import { page } from '$app/stores';
	import BottomNav from '../lib/components/BottomNav.svelte';
	import UnlockGate from '../lib/components/UnlockGate.svelte';
	import { isUnlocked } from '../lib/stores/session';

	// La page /settings gère elle-même le cas "pas encore de credentials" (premier lancement).
	$: skipGate = $page.url.pathname === '/settings';
</script>

<div class="page">
	{#if skipGate}
		<slot />
	{:else}
		<UnlockGate>
			<slot />
		</UnlockGate>
	{/if}
</div>

{#if $isUnlocked || skipGate}
	<BottomNav />
{/if}

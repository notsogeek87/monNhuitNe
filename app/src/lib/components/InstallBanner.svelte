<script lang="ts">
	import { onMount } from 'svelte';
	import { detectCapabilities, type NotificationCapability } from '../notifications/capabilities';

	let capability: NotificationCapability | null = null;
	let dismissed = false;

	onMount(() => {
		capability = detectCapabilities();
		dismissed = localStorage.getItem('monnhuitne:installBannerDismissed') === '1';
	});

	function dismiss() {
		dismissed = true;
		localStorage.setItem('monnhuitne:installBannerDismissed', '1');
	}
</script>

{#if capability?.shouldPromptInstall && !dismissed}
	<div class="banner card">
		<p>
			Sur iPhone, les notifications d'échec ne fonctionnent que si l'app est installée sur l'écran
			d'accueil : <strong>Partager → Sur l'écran d'accueil</strong>. Sans ça, l'app vérifie les échecs
			seulement quand vous l'ouvrez.
		</p>
		<button class="btn secondary" on:click={dismiss}>Compris</button>
	</div>
{/if}

<style>
	.banner {
		margin-bottom: 12px;
		font-size: 0.85rem;
	}
	.banner p {
		margin: 0 0 10px;
	}
</style>

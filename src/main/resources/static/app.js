const ingredientsInput = document.querySelector('#ingredientsInput');
const maxTimeSelect = document.querySelector('#maxTime');
const dietFilterSelect = document.querySelector('#dietFilter');
const searchBtn = document.querySelector('#searchBtn');
const statusEl = document.querySelector('#status');
const resultsEl = document.querySelector('#results');
const cardTemplate = document.querySelector('#recipeCardTemplate');

function setStatus(text) {
  statusEl.textContent = text;
}

function parseIngredients() {
  return ingredientsInput.value
    .split(',')
    .map((item) => item.trim().toLowerCase())
    .filter(Boolean);
}

function buildCard(recipe) {
  const node = cardTemplate.content.firstElementChild.cloneNode(true);
  const title = node.querySelector('.card-title');
  const image = node.querySelector('.card-image');
  const meta = node.querySelector('.meta');
  const summary = node.querySelector('.summary');
  const link = node.querySelector('.card-link');

  const dietText = recipe.veganFriendly
    ? 'Vegan-friendly'
    : recipe.vegetarianFriendly
      ? 'Vegetarian-friendly'
      : 'Mixed diet';

  title.textContent = recipe.name;
  image.src = recipe.image;
  image.alt = recipe.name;
  meta.textContent = `⏱ ~${recipe.estimatedMinutes} min · ${dietText}`;
  summary.textContent = recipe.summary || 'No instructions available.';
  link.href = recipe.sourceUrl || '#';

  return node;
}

async function searchRecipes() {
  const ingredients = parseIngredients();
  if (!ingredients.length) {
    setStatus('Please enter at least one ingredient.');
    resultsEl.innerHTML = '';
    return;
  }

  setStatus('Finding recipes...');
  searchBtn.disabled = true;
  resultsEl.innerHTML = '';

  try {
    const params = new URLSearchParams({
      ingredients: ingredients.join(','),
      maxTime: maxTimeSelect.value,
      diet: dietFilterSelect.value,
    });

    const response = await fetch(`/api/recipes/search?${params.toString()}`);
    const data = await response.json();

    if (!data.recipes?.length) {
      setStatus(data.message || 'No recipes found.');
      return;
    }

    data.recipes.forEach((recipe) => resultsEl.appendChild(buildCard(recipe)));
    setStatus(`${data.message}${data.cacheHit ? ' (served from PostgreSQL cache)' : ''}`);
  } catch (error) {
    console.error(error);
    setStatus('Could not load recipes right now. Please try again.');
  } finally {
    searchBtn.disabled = false;
  }
}

searchBtn.addEventListener('click', searchRecipes);

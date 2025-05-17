/**
 * SnapBid WebSocket Client
 * Handles real-time bidding communication
 */

let stompClient = null;
let auctionId = null;
let connected = false;

/**
 * Connect to the WebSocket server
 */
function connectWebSocket() {
    if (connected) {
        console.log("Already connected to WebSocket");
        return;
    }

    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    
    stompClient.connect({}, function(frame) {
        console.log('Connected to WebSocket: ' + frame);
        connected = true;
        
        // Get the auction ID from the page URL
        auctionId = getCurrentAuctionId();
        
        if (auctionId) {
            // Subscribe to updates for this specific auction
            stompClient.subscribe('/topic/auction/' + auctionId, function(message) {
                handleBidUpdate(JSON.parse(message.body));
            });
            console.log('Subscribed to auction updates for ID: ' + auctionId);
        } else {
            console.log('No auction ID found in URL, not subscribing to specific auction.');
        }
    }, function(error) {
        console.error('WebSocket connection error: ', error);
        connected = false;
        
        // Try to reconnect after a delay
        setTimeout(connectWebSocket, 5000);
    });
}

/**
 * Disconnect from WebSocket server
 */
function disconnectWebSocket() {
    if (stompClient !== null) {
        stompClient.disconnect();
        connected = false;
        console.log("Disconnected from WebSocket");
    }
}

/**
 * Send a bid via WebSocket
 */
function sendBid(bidAmount) {
    if (!connected || !stompClient) {
        console.error("Cannot send bid via WebSocket: Not connected");
        // Fall back to AJAX
        return sendBidViaAjax(bidAmount);
    }
    
    if (!auctionId) {
        console.error("Cannot send bid: No auction ID");
        return false;
    }

    const bidMessage = {
        auctionId: auctionId,
        bidAmount: bidAmount,
        bidderUsername: currentUsername // This should be set globally from the server
    };
    
    try {
        stompClient.send("/app/bid", {}, JSON.stringify(bidMessage));
        console.log("Bid sent via WebSocket: " + bidAmount);
        return true;
    } catch (error) {
        console.error("Error sending bid via WebSocket:", error);
        // Fall back to AJAX
        return sendBidViaAjax(bidAmount);
    }
}

/**
 * Send a bid via AJAX as fallback
 */
function sendBidViaAjax(bidAmount) {
    console.log("Falling back to AJAX for bid submission");
    
    if (!auctionId) {
        console.error("Cannot send bid: No auction ID");
        return false;
    }
    
    fetch(`/api/bids/${auctionId}`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]')?.getAttribute('content') || ''
        },
        body: JSON.stringify({
            amount: bidAmount
        })
    })
    .then(response => {
        if (!response.ok) {
            return response.json().then(data => {
                throw new Error(data.error || 'Failed to place bid');
            });
        }
        return response.json();
    })
    .then(data => {
        console.log("Bid placed successfully via AJAX:", data);
        // The WebSocket will automatically update all clients
    })
    .catch(error => {
        console.error("Error placing bid:", error);
        showError(error.message || 'Failed to place bid');
    });
    
    return true;
}

/**
 * Handle incoming bid updates
 */
function handleBidUpdate(bidMessage) {
    console.log("Received bid update: ", bidMessage);
    
    // Remove "no bids" message if it exists
    const noBidsMessage = document.getElementById('no-bids-message');
    if (noBidsMessage) {
        noBidsMessage.style.display = 'none';
    }
    
    // Update UI with new bid information
    updateBidDisplay(bidMessage);
    
    // Show notification
    showBidNotification(bidMessage);
}

/**
 * Update the bid display with new information
 */
function updateBidDisplay(bidMessage) {
    // Update current price display
    const currentPriceElement = document.getElementById('current-price');
    if (currentPriceElement) {
        currentPriceElement.textContent = formatCurrency(bidMessage.currentHighestBid);
    }
    
    // Update bid history if available
    const bidHistoryElement = document.getElementById('bid-history');
    if (bidHistoryElement) {
        const bidRow = document.createElement('div');
        bidRow.className = 'list-group-item bid-row';
        bidRow.innerHTML = `
            <div class="d-flex justify-content-between align-items-center">
                <div>
                    <strong class="bidder">${bidMessage.bidderUsername}</strong>
                    <span class="text-muted time">${bidMessage.timestamp}</span>
                </div>
                <span class="text-primary amount">${formatCurrency(bidMessage.bidAmount)}</span>
            </div>
        `;
        
        // Insert at the top
        if (bidHistoryElement.firstChild) {
            bidHistoryElement.insertBefore(bidRow, bidHistoryElement.firstChild);
        } else {
            bidHistoryElement.appendChild(bidRow);
        }
    }
    
    // Update minimum bid text
    const minimumBidElements = document.querySelectorAll('.form-text');
    minimumBidElements.forEach(element => {
        if (element.textContent.includes('Minimum bid:')) {
            const newMinBid = parseFloat(bidMessage.currentHighestBid) + 0.01;
            element.innerHTML = `Minimum bid: <span>${formatCurrency(newMinBid)}</span>`;
        }
    });
}

/**
 * Show a notification about a new bid
 */
function showBidNotification(bidMessage) {
    const notificationContainer = document.getElementById('bid-notifications');
    if (!notificationContainer) return;
    
    const notification = document.createElement('div');
    notification.className = 'bid-notification';
    notification.innerHTML = `
        <strong>${bidMessage.bidderUsername}</strong> placed a bid of 
        <strong>${formatCurrency(bidMessage.bidAmount)}</strong>
    `;
    
    notificationContainer.appendChild(notification);
    
    // Remove notification after 5 seconds
    setTimeout(() => {
        notification.classList.add('fade-out');
        setTimeout(() => {
            notificationContainer.removeChild(notification);
        }, 500);
    }, 5000);
}

/**
 * Show an error message
 */
function showError(message) {
    const errorContainer = document.createElement('div');
    errorContainer.className = 'alert alert-danger';
    errorContainer.textContent = message;
    
    const bidForm = document.getElementById('bid-form');
    if (bidForm) {
        bidForm.insertBefore(errorContainer, bidForm.firstChild);
        
        // Remove error after 5 seconds
        setTimeout(() => {
            errorContainer.remove();
        }, 5000);
    }
}

/**
 * Format a number as currency
 */
function formatCurrency(amount) {
    return '$' + parseFloat(amount).toFixed(2);
}

/**
 * Extract the auction ID from the URL
 */
function getCurrentAuctionId() {
    // Try to get the auction ID from the URL
    const urlPath = window.location.pathname;
    const matches = urlPath.match(/\/auctions\/(\d+)/);
    
    if (matches && matches.length > 1) {
        return matches[1];
    }
    
    // If not found in path, check for query parameter
    const urlParams = new URLSearchParams(window.location.search);
    const idParam = urlParams.get('id');
    
    // Check for data attribute on bid form
    const bidForm = document.getElementById('bid-form');
    if (bidForm && bidForm.dataset.auctionId) {
        return bidForm.dataset.auctionId;
    }
    
    return idParam;
}

// Connect when the page loads
document.addEventListener('DOMContentLoaded', function() {
    connectWebSocket();
    
    // Setup bid form submission if present
    const bidForm = document.getElementById('bid-form');
    if (bidForm) {
        bidForm.addEventListener('submit', function(e) {
            e.preventDefault();
            
            const bidInput = document.getElementById('bid-amount');
            if (bidInput && bidInput.value) {
                const bidAmount = parseFloat(bidInput.value);
                if (!isNaN(bidAmount) && bidAmount > 0) {
                    sendBid(bidAmount);
                    bidInput.value = '';
                } else {
                    showError('Please enter a valid bid amount');
                }
            } else {
                showError('Please enter a bid amount');
            }
        });
    }
});

// Disconnect when navigating away
window.addEventListener('beforeunload', function() {
    disconnectWebSocket();
}); 
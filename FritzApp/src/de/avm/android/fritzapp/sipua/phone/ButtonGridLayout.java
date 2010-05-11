/*
 * Copyright (C) 2010 AVM GmbH <info@avm.de>
 * Copyright (C) 2009 The Sipdroid Open Source Project
 * Copyright (C) 2006 The Android Open Source Project
 * 
 * This file is part of Sipdroid (http://www.sipdroid.org)
 * 
 * Sipdroid is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This source code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this source code; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package de.avm.android.fritzapp.sipua.phone;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

public class ButtonGridLayout extends ViewGroup {

    private final int mColumns = 3;
    private int mPaddingBottom = 0,mPaddingLeft = 0,mPaddingRight = 0,mPaddingTop = 0;
    
    public ButtonGridLayout(Context context) {
        super(context);
    }

    public ButtonGridLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ButtonGridLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b)
    {
        final int rows = getRows();
        final View child0 = getChildAt(0);
        final int childWidth = child0.getMeasuredWidth();
        final int childHeight = child0.getMeasuredHeight();

        int yGap = ((getHeight() - mPaddingTop - mPaddingBottom) -
        		rows * childHeight) / (rows - 1);
        int xGap = ((getWidth() - mPaddingLeft - mPaddingRight) -
        		mColumns * childWidth) / (mColumns - 1);
        
        for (int row = 0; row < rows; row++)
        {
        	int yPos = mPaddingTop + (childHeight + yGap) * row;
            for (int col = 0; col < mColumns; col++)
            {
                int cell = row * mColumns + col;
                if (cell >= getChildCount()) break;
                
                View child = getChildAt(cell);
            	int xPos = mPaddingLeft + (childWidth + xGap) * col;
                child.layout(xPos, yPos, xPos + childWidth, yPos + childHeight);
            }
        }
    }

    private int getRows() {
        return (getChildCount() + mColumns - 1) / mColumns; 
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = mPaddingLeft + mPaddingRight;
        int height = mPaddingTop + mPaddingBottom;
        
        // Measure the first child and get it's size
        View child = getChildAt(0);
        child.measure(MeasureSpec.UNSPECIFIED , MeasureSpec.UNSPECIFIED);
        int childWidth = child.getMeasuredWidth();
        int childHeight = child.getMeasuredHeight();
        // Make sure the other children are measured as well, to initialize
        for (int i = 1; i < getChildCount(); i++) {
            getChildAt(0).measure(MeasureSpec.UNSPECIFIED , MeasureSpec.UNSPECIFIED);
        }
        // All cells are going to be the size of the first child
        width += mColumns * childWidth;
        height += getRows() * childHeight;
        
        width = resolveSize(width, widthMeasureSpec);
        height = resolveSize(height, heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

}
